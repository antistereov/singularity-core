package io.stereov.singularity.auth.geolocation.service

import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.CityResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.geolocation.exception.GeoLocationException
import io.stereov.singularity.auth.geolocation.properties.GeolocationProperties
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.invoke
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import java.io.BufferedOutputStream
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
    private fun initialize() = runBlocking{
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
            DatabaseReader.Builder(cityDbFile).build()
        } catch(e: Exception) {
            throw GeoLocationException("GeoLite2-City database could not be initialized", e)
        }

        logger.info { "Successfully initialized GeoLite2-City database" }
    }

    private suspend fun shouldUpdate(): Boolean {
        if (properties.accountId == null) throw GeoLocationException("Cannot update GeoLite2-City database: account ID is not set")
        if (properties.licenseKey == null) throw GeoLocationException("Cannot update GeoLite2-City database: license key is not set")

        val responseHeaders = webClient.head()
            .uri(URI(downloadUrl))
            .headers { it.setBasicAuth(properties.accountId, properties.licenseKey) }
            .retrieve()
            .toBodilessEntity()
            .awaitSingle()
            .headers

        val lastModifiedHeader = responseHeaders["Last-Modified"]?.firstOrNull()

        if (lastModifiedHeader == null) return true

        val remoteLastModified = ZonedDateTime.parse(lastModifiedHeader, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()

        if (!cityDbFile.exists()) return true
        val localLastModified = Files.getLastModifiedTime(cityDbFile.toPath()).toInstant()

        return remoteLastModified.isAfter(localLastModified)
    }

    private suspend fun download() {
        logger.info { "Downloading GeoLite2-City database" }

        if (properties.accountId == null) throw GeoLocationException("Cannot download GeoLite2-City database: account ID is not set")
        if (properties.licenseKey == null) throw GeoLocationException("Cannot download GeoLite2-City database: license key is not set")


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
            val inputStream = dataBuffer.asInputStream()

            Dispatchers.IO {
                GZIPInputStream(inputStream).use { gzipInput ->
                    BufferedOutputStream(
                        Files.newOutputStream(
                            cityDbFile.toPath(),
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING
                        )
                    )
                        .use { fileOutput ->
                            gzipInput.copyTo(fileOutput)
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
            throw GeoLocationException("GeoIP2-City database is not initialized. Cannot resolve IP address $ipAddress")
        }

        return@withContext response
    }
}