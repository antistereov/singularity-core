package io.stereov.singularity.auth.geolocation.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
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

/**
 * Service for managing and using the MaxMind GeoLite2-City database to retrieve geolocation information
 * such as city details based on IP addresses. This service also handles database updates, initialization,
 * and proper maintenance of the database files.
 */
@Service
class GeolocationDatabaseService(
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
         updateDatabase().onFailure {
             logger.warn(it) { "Failed to update geolocation database: ${it.message}"}
         }
    }

    @Scheduled(cron = "0 0 3 * */2 *")
    private fun update() = runBlocking{
        if (properties.download) updateDatabase().onFailure {
            logger.warn(it) { "Failed to update geolocation database: ${it.message}"}
        }
    }

    /**
     * Updates the GeoLite2-City database by ensuring it exists and is up to date based on the provided configurations.
     * If the database is outdated or missing and automated download is enabled, it downloads the latest version of the database
     * and initializes the database reader.
     *
     * @return A [Result] containing [Unit] on success, or a [GeolocationException] if any error occurs during the update process.
     */
    private suspend fun updateDatabase(): Result<Unit, GeolocationException> {
        if (!properties.enabled) return Ok(Unit)

        return coroutineBinding {
            Files.createDirectories(Path(properties.databaseDirectory))

            if (properties.download) {
                if (shouldUpdate().bind()) {
                    logger.info { "GeoLite2-City.mmdb does not exist or is out of date. Starting download..." }
                    download()
                } else {
                    logger.info { "Local GeoLite2-City.mmdb is up to date" }
                }
            } else {
                logger.info { "Automated download of GeoLite2-City database is disabled" }
            }

            val downloaded = runCatching {
                DatabaseReader.Builder(cityDbFile).withCache(CHMCache()).build()
            }
                .mapError { ex -> GeolocationException.Init("Failed to initialize city db: ${ex.message}", ex) }
                .bind()

            cityDb = downloaded

            logger.info { "Successfully initialized GeoLite2-City database" }
        }
    }

    /**
     * Determines whether the GeoLite2-City database should be updated.
     *
     * The method checks authentication credentials and compares the last modified timestamp
     * of the remote database with the local database file, if it exists. If any required credentials
     * are missing or the remote database is more recent, the method will indicate an update is needed.
     *
     * @return [Result] containing a [Boolean] value indicating whether an update is required (`true` for update needed, `false` otherwise)
     *  or a [GeolocationException] in case of errors such as authentication failure, too many requests,
     *  or issues accessing the local or remote database.
     */
    private suspend fun shouldUpdate(): Result<Boolean, GeolocationException> {
        if (properties.accountId == null) {
            return Err(GeolocationException.Authentication("Cannot update GeoLite2-City database: account ID is not set"))
        }
        if (properties.licenseKey == null) {
            return Err(GeolocationException.Authentication("Cannot update GeoLite2-City database: license key is not set"))
        }

        return coroutineBinding {
            val responseHeaders = runCatching {
                webClient.head()
                    .uri(URI(downloadUrl))
                    .headers { it.setBasicAuth(properties.accountId, properties.licenseKey) }
                    .retrieve()
                    .onStatus({ it == HttpStatus.TOO_MANY_REQUESTS }) {
                        Mono.error(GeolocationException.TooManyRequests("Cannot fetch database: too many request for your account"))
                    }
                    .onStatus({ it == HttpStatus.UNAUTHORIZED }) {
                        Mono.error(GeolocationException.Authentication("Database download unauthorized"))
                    }
                    .toBodilessEntity()
                    .awaitSingle()
                    .headers
            }
                .mapError { ex ->
                    when (ex) {
                        is GeolocationException -> ex
                        else -> GeolocationException.Database("Database download failed: ${ex.message}", ex)
                    }
                }
                .bind()

            val lastModifiedHeader = responseHeaders["Last-Modified"]?.firstOrNull()
                ?: return@coroutineBinding true

            val remoteLastModified = runCatching {
                ZonedDateTime.parse(lastModifiedHeader, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()
            }.getOrElse { return@coroutineBinding true }

            if (!cityDbFile.exists()) return@coroutineBinding true

            val localLastModified = runCatching {
                Files.getLastModifiedTime(cityDbFile.toPath()).toInstant()
            }.getOrElse { return@coroutineBinding true }

            runCatching {
                remoteLastModified.isAfter(localLastModified)
            }.getOrElse { true }
        }
    }

    /**
     * Downloads the GeoLite2-City database from the specified source.
     * This method ensures necessary authentication using the account ID
     * and license key provided in the properties. If authentication parameters
     * are missing or the download fails, appropriate exceptions are returned.
     *
     * @return A [Result] containing [Unit] on successful download and save,
     *   or a [GeolocationException] instance in case of errors such as
     *   missing authentication information or download/save failures.
     */
    private suspend fun download(): Result<Unit, GeolocationException> {
        logger.info { "Downloading GeoLite2-City database" }

        if (properties.accountId == null) {
            return Err(GeolocationException.Authentication("Cannot update GeoLite2-City database: account ID is not set"))
        }
        if (properties.licenseKey == null) {
            return Err(GeolocationException.Authentication("Cannot update GeoLite2-City database: license key is not set"))
        }

        return coroutineBinding {
            val dataBuffer = runCatching {
                DataBufferUtils.join(
                    webClient.get()
                        .uri(URI(downloadUrl))
                        .headers { headers ->  headers.setBasicAuth(properties.accountId, properties.licenseKey) }
                        .retrieve()
                        .bodyToFlux<DataBuffer>()
                ).awaitSingle()
            }
                .mapError { ex -> GeolocationException.Database("Failed to download database: ${ex.message}", ex) }
                .bind()

            logger.info { "Successfully downloaded GeoLite2-City database" }

            saveDatabase(dataBuffer).bind()
        }
    }

    /**
     * Saves the database file by extracting and processing the provided data buffer. The method handles
     * GZIP and TAR archive formats to locate and save the database file with a `.mmdb` extension.
     *
     * @param dataBuffer The buffer containing the database file in a compressed format.
     * @return A [Result] that contains [Unit] on success or [GeolocationException.Save] if an error occurs during the save process.
     */
    private suspend fun saveDatabase(dataBuffer: DataBuffer): Result<Unit, GeolocationException.Save> = withContext(Dispatchers.IO) {
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
        } catch (e: Throwable) {
            return@withContext Err(GeolocationException.Save("Failed to save databased to path $cityDbFile: ${e.message}", e))
        } finally {
            DataBufferUtils.release(dataBuffer)
            logger.info { "Successfully extracted and saved database" }
        }

        return@withContext Ok(Unit)
    }

    /**
     * Retrieves city information from the GeoIP2-City database for the given IP address.
     *
     * @param ipAddress The IP address for which the city information is to be retrieved.
     * @return A [Result] wrapping [CityResponse] if successful, or a [GeolocationException] if an error occurs.
     */
    suspend fun getCity(ipAddress: InetAddress): Result<CityResponse, GeolocationException> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            cityDb?.city(ipAddress)
        }
            .mapError { ex -> GeolocationException.Get("Failed to get geolocation for IP address $ipAddress: ${ex.message}", ex) }
            .andThen { it .toResultOr { GeolocationException.Init("GeoIP2-City database is not initialized. Cannot resolve IP address $ipAddress") }  }
    }
}
