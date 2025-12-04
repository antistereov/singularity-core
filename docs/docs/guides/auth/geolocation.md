---
sidebar_position: 7
description: Use geolocation data to secure access.
---

# Geolocation

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

*Singularity* integrates automated geolocation resolving for incoming requests. When enabled and configured correctly, 
it uses the **MaxMind GeoLite2-City** database to look up the geographical location associated with a client's IP address.

This database is completely free to use and does not have any usage limits.
The only thing you need to do is to create a free [MaxMind account](https://support.maxmind.com/hc/en-us/articles/4407099783707-Create-an-Account#h_01G4G4NV169TJWFCJ1KGFAM1CD) 
and provide your [account ID and license key](https://support.maxmind.com/hc/en-us/articles/4407111582235-Generate-a-License-Key).

When enabled, the geolocation of every session used for login or registration will be stored. 
This enhances security by allowing users to review and revoke access to sessions originating from unknown locations.

## Highlights

* No usage limits when using MaxMind GeoLite2-City.
* Automatic database download and updates.
* Built-in integration with login/registration flows.
* Useful for detecting unusual login locations and improving account security.

## Usage with `GeolocationService`

The **`GeolocationService`** is the entry point for retrieving geolocation data within your application logic. It handles the entire process: extracting the client's IP address from the request, querying the local MaxMind database, and mapping the result into a clean response object.

### Core Method

The primary method you will use in your controllers or services is `getLocationOrNull`.

| Method                  | Description                                                          | Kotlin Signature                                                                   |
|:------------------------|:---------------------------------------------------------------------|:-----------------------------------------------------------------------------------|
| **`getLocationOrNull`** | Retrieves the geolocation details for the client making the request. | `suspend fun getLocationOrNull(exchange: ServerWebExchange): GeolocationResponse?` |

### Example

This is a typical pattern for using the service in a controller:

```kotlin
@RestController
class SessionController(
    private val geolocationService: GeolocationService
) {
    @GetMapping("/api/my-session-location")
    suspend fun getSessionLocation(exchange: ServerWebExchange): GeolocationResponse? {

        // Get the geolocation data based on the client's IP in the request
        val geolocation = geolocationService.getLocationOrNull(exchange)

        // GeolocationResponse? can be null if:
        // 1. The client's IP address could not be reliably determined.
        // 2. The geolocation database lookup failed.

        return geolocation
    }
}
```

## Geolocation Data Structure

The **`GeolocationResponse`** data transfer object (DTO) is what the `GeolocationService` returns. It encapsulates all the relevant geographical information provided by the MaxMind database.

```kotlin
data class GeolocationResponse(
    val ipAddress: String,
    val city: City,
    val country: Country,
    val continent: Continent,
    val location: Location
)
```

| Field           | Type                                  | Description                                                  |
|:----------------|:--------------------------------------|:-------------------------------------------------------------|
| **`ipAddress`** | `String`                              | The IP address that was resolved.                            |
| **`city`**      | `com.maxmind.geoip2.record.City`      | Detailed city information (e.g., city name, postal code).    |
| **`country`**   | `com.maxmind.geoip2.record.Country`   | Detailed country information (e.g., country name, ISO code). |
| **`continent`** | `com.maxmind.geoip2.record.Continent` | Detailed continent information.                              |
| **`location`**  | `com.maxmind.geoip2.record.Location`  | Geographical coordinates (latitude, longitude) and timezone. |

The fields within `GeolocationResponse` (`City`, `Country`, `Continent`, `Location`) are the standard record types from the **MaxMind GeoIP2 library**. 
You can consult the MaxMind documentation for a full list of available properties on these objects.

## Configuration

Geolocation resolving is controlled by the following configuration properties.

| Property                                          | Type      | Description                                                                                                                                          | Default value         |
|:--------------------------------------------------|:----------|:-----------------------------------------------------------------------------------------------------------------------------------------------------|:----------------------|
| `singularity.auth.geolocation.enabled`            | `Boolean` | Enable the geolocation service.                                                                                                                      | `false`               |
| `singularity.auth.geolocation.real-ip-header`     | `String`  | The preferred HTTP header used for identifying the real IP address of the client (e.g., when behind a proxy/load balancer).                          | `X-Real-IP`           |
| `singularity.auth.geolocation.database-directory` | `String`  | The local directory where the GeoLite2-City database will be saved to.                                                                               | `./.data/geolocation` |
| `singularity.auth.geolocation.database-filename`  | `String`  | The filename of the GeoLite2-City database.                                                                                                          | `GeoLite2-City.mmdb`  |
| `singularity.auth.geolocation.download`           | `Boolean` | **Enable automated downloads and updates** of the database. Requires `account-id` and `license-key`. If `false`, you must provide the file manually. | `true`                |
| `singularity.auth.geolocation.account-id`         | `String`  | The **Account ID** of your MaxMind account. Required for automated database download.                                                                |                       |
| `singularity.auth.geolocation.license-key`        | `String`  | The **License Key** of your MaxMind account. Required for automated database download.                                                               |                       |

### Example Configuration

```yaml
singularity:
  auth:
    geolocation:
      enabled: true
      download: true
      account-id: "YOUR_MAXMIND_ACCOUNT_ID" # <-- Required for download: true
      license-key: "YOUR_MAXMIND_LICENSE_KEY" # <-- Required for download: true
```