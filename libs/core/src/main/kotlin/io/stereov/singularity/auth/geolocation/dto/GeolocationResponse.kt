package io.stereov.singularity.auth.geolocation.dto

import com.maxmind.geoip2.record.City
import com.maxmind.geoip2.record.Continent
import com.maxmind.geoip2.record.Country
import com.maxmind.geoip2.record.Location

/**
 * Represents a geolocation response containing details about a specific IP address.
 *
 * This data class encapsulates geolocation information including the IP address,
 * associated city, country, continent, and location details. It is typically used
 * as a response object when retrieving geolocation information for a given IP address.
 *
 * @param ipAddress The IP address for which geolocation information is associated.
 * @param city The city details associated with the geolocation of the IP address.
 * @param country The country details corresponding to the geolocation.
 * @param continent The continent details related to the geolocation.
 * @param location Additional geographical location details such as latitude and longitude.
 */
data class GeolocationResponse(
    val ipAddress: String,
    val city: City,
    val country: Country,
    val continent: Continent,
    val location: Location
)
