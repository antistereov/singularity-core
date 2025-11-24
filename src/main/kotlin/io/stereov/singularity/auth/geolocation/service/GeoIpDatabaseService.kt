package io.stereov.singularity.auth.geolocation.service

import com.maxmind.db.CHMCache
import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.CityResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.geolocation.exception.GeolocationException
import io.stereov.singularity.auth.geolocation.properties.GeolocationProperties
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Mono
import java.io.File
import java.net.InetAddress
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPInputStream
import kotlin.io.path.Path

@Service
class GeoIpDatabaseService(
    private val properties: GeolocationProperties,
    private val webClient: WebClient
) {

    private var cityDb: DatabaseReader? = null

    private val cityDbFile = File(properties.databaseDirectory)
        .resolve(properties.databaseFilename)
    private val logger = KotlinLogging.logger {}

    private val downloadUrl = "https://download.maxmind.com/geoip/databases/GeoLite2-City/download?suffix=tar.gz"

    @PostConstruct
    fun initialize() = runBlocking {
         updateDatabase()
    }

    @Scheduled(cron = "0 0 3 * */2 *")
    private fun update() = runBlocking{
        if (properties.download) updateDatabase()
    }

    private suspend fun updateDatabase() {
        if (!properties.enabled) return

        Files.createDirectories(Path(properties.databaseDirectory))

        if (properties.download) {
            if (shouldUpdate()) {
                logger.info { "GeoLite2-City.mmdb does not exist or is out of date. Starting download..." }
                download()
            } else {
                logger.info { "Local GeoLite2-City.mmdb is up to date" }
            }
        } else {
            logger.info { "Automated download of GeoLite2-City database is disabled" }
        }

        cityDb = try {
            DatabaseReader.Builder(cityDbFile).withCache(CHMCache()).build()
        } catch(e: Exception) {
            logger.warn(e) { "GeoLite2-City database could not be initialized" }
            null
        }

        logger.info { "Successfully initialized GeoLite2-City database" }
    }

    private suspend fun shouldUpdate(): Boolean {
        if (properties.accountId == null) throw GeolocationException("Cannot update GeoLite2-City database: account ID is not set")
        if (properties.licenseKey == null) throw GeolocationException("Cannot update GeoLite2-City database: license key is not set")

        val responseHeaders = try {
            webClient.head()
                .uri(URI(downloadUrl))
                .headers { it.setBasicAuth(properties.accountId, properties.licenseKey) }
                .retrieve()
                .onStatus({ it == HttpStatus.TOO_MANY_REQUESTS}) {
                    Mono.error(TooManyRequestsException("Cannot fetch database: too many request for your account"))
                }
                .onStatus( { it == HttpStatus.UNAUTHORIZED }) {
                    Mono.error(GeolocationException("Database download unauthorized"))
                }
                .toBodilessEntity()
                .awaitSingle()
                .headers
        } catch (e: TooManyRequestsException) {
            logger.warn(e) { "Database download failed: ${e.message}" }
            return false
        } catch (e: GeolocationException) {
            logger.warn(e) { "Database download failed: ${e.message}" }
            return false
        }

        val lastModifiedHeader = responseHeaders["Last-Modified"]?.firstOrNull() ?: return true

        val remoteLastModified = ZonedDateTime.parse(lastModifiedHeader, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()

        if (!cityDbFile.exists()) return true
        val localLastModified = Files.getLastModifiedTime(cityDbFile.toPath()).toInstant()

        return remoteLastModified.isAfter(localLastModified)
    }

    private suspend fun download() {
        logger.info { "Downloading GeoLite2-City database" }

        if (properties.accountId == null) throw GeolocationException("Cannot download GeoLite2-City database: account ID is not set")
        if (properties.licenseKey == null) throw GeolocationException("Cannot download GeoLite2-City database: license key is not set")


        val dataBuffer = DataBufferUtils.join(
            webClient.get()
            .uri(URI(downloadUrl))
            .headers { headers ->  headers.setBasicAuth(properties.accountId, properties.licenseKey) }
            .retrieve()
            .bodyToFlux<DataBuffer>()
        ).awaitSingle()

        logger.info { "Successfully downloaded GeoLite2-City database" }

        saveDatabase(dataBuffer)
    }

    private suspend fun saveDatabase(dataBuffer: DataBuffer) = withContext(Dispatchers.IO) {
        logger.info { "Extracting and saving database to ${cityDbFile.absolutePath}" }

        try {
            dataBuffer.asInputStream().use { input ->
                GZIPInputStream(input).use { gzipInput ->
                    TarArchiveInputStream(gzipInput).use { tarIn ->
                        var entry = tarIn.nextEntry
                        while (entry != null) {
                            if (!entry.isDirectory && entry.name.endsWith(".mmdb")) {
                                logger.debug { "Found ${entry.name}, extracting..." }
                                Files.newOutputStream(
                                    cityDbFile.toPath(),
                                    StandardOpenOption.CREATE,
                                    StandardOpenOption.TRUNCATE_EXISTING
                                ).use { fileOutput ->
                                    tarIn.copyTo(fileOutput)
                                }
                                break
                            }
                            entry = tarIn.nextEntry
                        }
                    }
                }
            }
        } finally {
            DataBufferUtils.release(dataBuffer)
        }

        logger.info { "Successfully extracted and saved database" }
    }

    suspend fun getCity(ipAddress: InetAddress): CityResponse = withContext(Dispatchers.IO) {
        val response = cityDb?.city(ipAddress)

        if (response == null) {
            throw GeolocationException("GeoIP2-City database is not initialized. Cannot resolve IP address $ipAddress")
        }

        return@withContext response
    }
}
