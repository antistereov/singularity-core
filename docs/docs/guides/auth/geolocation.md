---
sidebar_position: 6
description: Use geolocation data to secure access.
---

# Geolocation

:::note
This guide assumes familiarity with the [Spring Framework](https://spring.io).  
If you are new to Spring, we recommend starting with their [official guides](https://spring.io/quickstart) to get up to speed.
:::

*Singularity* integrates automated geolocation resolving for requests.
When enabled and configured correctly, it downloads the latest version of the [MaxMind GeoLite2-City](https://dev.maxmind.com/geoip/geolite2-free-geolocation-data/) database.

This database is completely free to use and does not have any usage limits.
The only thing you need to do is to create a free [MaxMind account](https://support.maxmind.com/hc/en-us/articles/4407099783707-Create-an-Account#h_01G4G4NV169TJWFCJ1KGFAM1CD) 
and provide your [account ID and license key](https://support.maxmind.com/hc/en-us/articles/4407111582235-Generate-a-License-Key).

When enabled, the geolocation of every session that is used for login or registration will be stored.
This can enhance security since users can revoke access to sessions that they do not know.

## Highlights

* No usage limits when using MaxMind GeoLite2-City
* Automatic database download and updates
* Built-in integration with login/registration flows
* Useful for detecting unusual login locations and improving account security

## Usage

:::info
Geolocation data is included in **login** and **registration** responses,  
as well as in your **access logs**.
:::

If you need to access the location of your users, you can use the [GeolocationService](https://github.com/antistereov/singularity-core/blob/669bd23c2648ab5ed4b9bceb641d5374dd69bfef/src/main/kotlin/io/stereov/singularity/auth/geolocation/service/GeolocationService.kt).

```kotlin

@Service
class CoolService(private val geolocationService: GeolocationService) {

    /**
     * This method returns geolocation-specific responses if the current location can be identified. 
     */
    suspend fun locationSpecific(exchange: ServerWebExchange): CoolStuff {

        /**
         * This will return the geolocation if it can be resolved.
         */
        val location = geoLocationService.getLocationOrNull(exchange)
            ?: return CoolStuff.forEveryone()

        return CoolStuff.byLocation(location)
    }
}

@RestController
@RequestMapping("/api/cool-stuff")
class CoolController(private val service: CoolService) {

    /**
     * Spring will automatically fill the exchange parameter when the request is executed.
     */
    @GetMapping("/location-specific")
    suspend fun locationSpecific(exchange: ServerWebExchange): ResponseEntity<CoolStuff> {
        return ResponseEntity.ok(service.locationSpecific(exchange))
    }

    @GetMapping("/location-specific-default")
    suspend fun locationSpecificWithDefault(exchange: ServerWebExchange): ResponseEntity<CoolStuff> {
        return ResponseEntity.ok(service.locationSpecificWithDefault(exchange))
    }
}
```
## Configuration

| Property                                        | Type      | Description                                                                                                                                                          | Default value         |
|-------------------------------------------------|-----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------------------|
| singularity.auth.geolocation.enabled            | `Boolean` | Enable resolving IP addresses to geolocations. This will download the GeoLite2-City database from maxmind. Account ID and license key are required for the download. | `false`               |
| singularity.auth.geolocation.real-ip-header     | `String`  | The preferred header used for identification of the real IP address of the client.                                                                                   | `X-Real-IP`           |
| singularity.auth.geolocation.database-directory | `String`  | The directory where the GeoLite2-City database will be saved to.                                                                                                     | `./.data/geolocation` |
| singularity.auth.geolocation.database-filename  | `String`  | The filename of the GeoLite2-City database. Defaults to GeoLite2-City.mmdb.                                                                                          | `GeoLite2-City.mmdb`  |
| singularity.auth.geolocation.download           | `Boolean` | Enable automated downloads and updates of the database. You can also provide this file yourself.                                                                     | `true`                |
| singularity.auth.geolocation.account-id         | `String`  | The account ID of your maxmind account. Required to download the GeoLite2-City database to resolve your geolocation.                                                 |                       |
| singularity.auth.geolocation.license-key        | `String`  | The license key of your maxmind account. Required to download the GeoLite2-City database to resolve your geolocation.                                                |                       |

