package io.stereov.singularity.auth.geolocation

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.dto.request.LoginRequest
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.io.File
import java.io.FileNotFoundException
import java.net.InetAddress
import java.net.URL

class GeolocationServiceTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var geoLocationService: GeolocationService

    companion object {
        val resourceUrl: URL = object {}.javaClass.classLoader.getResource("geolocation/GeoLite2-City.mmdb")
            ?: throw FileNotFoundException("GeoLite2-City.mmdb is not found in class path")

        val resourceFile = File(resourceUrl.toURI())
        val dir: File = resourceFile.parentFile

        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("singularity.auth.geolocation.enabled") { true }
            registry.add("singularity.auth.geolocation.database-directory") { dir.absolutePath.toString() }
            registry.add("singularity.auth.geolocation.download") { false }
        }
    }

    @Test fun `should resolve geolocation`() = runTest {
        val city = geoLocationService.getCityResponse(InetAddress.getByName("8.8.8.8")).getOrThrow()
        
        assertThat(city.toString()).isEqualTo("CityResponse[city=City[locales=[en], confidence=null, geonameId=null, names={}], continent=Continent[locales=[en], code=NA, geonameId=6255149, names={ja=北アメリカ, pt-BR=América do Norte, en=North America, zh-CN=北美洲, fr=Amérique du Nord, es=Norteamérica, de=Nordamerika, ru=Северная Америка}], country=Country[locales=[en], confidence=null, geonameId=6252001, isInEuropeanUnion=false, isoCode=US, names={ja=アメリカ, pt-BR=EUA, en=United States, zh-CN=美国, fr=États Unis, es=Estados Unidos, de=USA, ru=США}], location=Location[accuracyRadius=1000, averageIncome=null, latitude=37.751, longitude=-97.822, populationDensity=null, timeZone=America/Chicago], maxmind=MaxMind[queriesRemaining=null], postal=Postal[code=null, confidence=null], registeredCountry=Country[locales=[en], confidence=null, geonameId=6252001, isInEuropeanUnion=false, isoCode=US, names={ja=アメリカ, pt-BR=EUA, en=United States, zh-CN=美国, fr=États Unis, es=Estados Unidos, de=USA, ru=США}], representedCountry=RepresentedCountry[locales=[en], confidence=null, geonameId=null, isInEuropeanUnion=false, isoCode=null, names={}, type=null], subdivisions=[], traits=Traits[autonomousSystemNumber=null, autonomousSystemOrganization=null, connectionType=null, domain=null, ipAddress=/8.8.8.8, isAnonymous=false, isAnonymousVpn=false, isAnycast=false, isHostingProvider=false, isLegitimateProxy=false, isPublicProxy=false, isResidentialProxy=false, isTorExitNode=false, ipRiskSnapshot=null, isp=null, mobileCountryCode=null, mobileNetworkCode=null, network=8.8.8.0/23, organization=null, userType=null, userCount=null, staticIpScore=null]]")
    }

    @Test fun `it should resolve geolocation in session`() = runTest {
        val user = registerUser()
        user.info.clearSessions()
        userService.save(user.info)

        val body = LoginRequest(email = user.email!!, password = user.password!!)

        webTestClient.post()
            .uri("/api/auth/login")
            .header("X-Real-IP", "93.128.176.188")
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk

        val updatedUser = userService.findById(user.id).getOrThrow()
        val location = updatedUser.sensitive.sessions.values.first().location

        println(location)

        assertThat(location?.cityName).isEqualTo("Berlin")
        assertThat(location?.countryCode).isEqualTo("DE")
    }
}
