package io.stereov.singularity.auth.geolocation.model

/**
 * # GeoLocationResponse data class.
 *
 * This data class represents the response from a geolocation service.
 * It contains various properties that provide information about the geographical location of an IP address.
 *
 * @property ipVersion The version of the IP address (e.g., IPv4 or IPv6).
 * @property ipAddress The IP address being queried.
 * @property latitude The latitude of the geographical location.
 * @property longitude The longitude of the geographical location.
 * @property countryName The name of the country associated with the IP address.
 * @property countryCode The ISO code of the country associated with the IP address.
 * @property timeZone The time zone of the geographical location.
 * @property zipCode The ZIP code of the geographical location (nullable).
 * @property cityName The name of the city associated with the IP address.
 * @property regionName The name of the region associated with the IP address.
 * @property continent The name of the continent associated with the IP address.
 * @property continentCode The ISO code of the continent associated with the IP address.
 * @property isProxy Indicates whether the IP address is a proxy.
 * @property language The language associated with the geographical location (nullable).
 * @property timeZones A list of time zones associated with the geographical location (nullable).
 * @property tlds A list of top-level domains (TLDs) associated with the geographical location (nullable).
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
data class GeoLocationResponse(
    val ipVersion: Int,
    val ipAddress: String,
    val latitude: Double,
    val longitude: Double,
    val countryName: String,
    val countryCode: String,
    val timeZone: String,
    val zipCode: String?,
    val cityName: String,
    val regionName: String,
    val continent: String,
    val continentCode: String,
    val isProxy: Boolean,
    val language: String?,
    val timeZones: List<String>?,
    val tlds: List<String>?
)
