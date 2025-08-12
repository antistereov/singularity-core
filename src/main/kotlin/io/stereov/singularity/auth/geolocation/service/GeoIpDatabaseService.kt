package io.stereov.singularity.auth.geolocation.service

import com.maxmind.geoip2.DatabaseReader
import com.maxmind.geoip2.model.CityResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.geolocation.exception.GeoLocationException
import io.stereov.singularity.auth.geolocation.properties.GeolocationProperties
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.io.File
import java.net.InetAddress

@Service
class GeoIpDatabaseService(
    private val properties: GeolocationProperties
) {

    private var cityDb: DatabaseReader? = null
    private val cityDbFile = File(properties.databaseDirectory)
        .resolve("GeoLite2-City.mmdb")

    private val logger = KotlinLogging.logger {}

    @PostConstruct
    private fun initialize() {
        if (!properties.enabled) return

        cityDb = try {
            DatabaseReader.Builder(cityDbFile).build()
        } catch(e: Exception) {
            throw GeoLocationException("GeoLite2-City database could not be initialized", e)
        }

        logger.info { "Successfully initializes GeoLite2-City database" }
    }

    suspend fun getCity(ipAddress: InetAddress): CityResponse = withContext(Dispatchers.IO) {
        val response = cityDb?.city(ipAddress)

        if (response == null) {
            throw GeoLocationException("GeoIP2-City database is not initialized. Cannot resolve IP address $ipAddress")
        }

        return@withContext response
    }
}