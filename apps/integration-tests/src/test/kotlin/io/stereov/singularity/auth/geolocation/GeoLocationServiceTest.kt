package io.stereov.singularity.auth.geolocation

import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.test.BaseIntegrationTest
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.dto.request.RegisterUserRequest
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

class GeoLocationServiceTest : BaseIntegrationTest() {

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
        val city = geoLocationService.getLocation(InetAddress.getByName("8.8.8.8"))
        
        assertThat(city.toString()).isEqualTo("com.maxmind.geoip2.model.CityResponse [ {\"city\":{},\"continent\":{\"code\":\"NA\",\"geoname_id\":6255149,\"names\":{\"de\":\"Nordamerika\",\"ru\":\"Северная Америка\",\"pt-BR\":\"América do Norte\",\"ja\":\"北アメリカ\",\"en\":\"North America\",\"fr\":\"Amérique du Nord\",\"zh-CN\":\"北美洲\",\"es\":\"Norteamérica\"}},\"country\":{\"geoname_id\":6252001,\"is_in_european_union\":false,\"iso_code\":\"US\",\"names\":{\"de\":\"USA\",\"ru\":\"США\",\"pt-BR\":\"EUA\",\"ja\":\"アメリカ\",\"en\":\"United States\",\"fr\":\"États Unis\",\"zh-CN\":\"美国\",\"es\":\"Estados Unidos\"}},\"location\":{\"accuracy_radius\":1000,\"latitude\":37.751,\"longitude\":-97.822,\"time_zone\":\"America/Chicago\"},\"maxmind\":{},\"postal\":{},\"registered_country\":{\"geoname_id\":6252001,\"is_in_european_union\":false,\"iso_code\":\"US\",\"names\":{\"de\":\"USA\",\"ru\":\"США\",\"pt-BR\":\"EUA\",\"ja\":\"アメリカ\",\"en\":\"United States\",\"fr\":\"États Unis\",\"zh-CN\":\"美国\",\"es\":\"Estados Unidos\"}},\"represented_country\":{\"is_in_european_union\":false},\"traits\":{\"ip_address\":\"8.8.8.8\",\"is_anonymous\":false,\"is_anonymous_proxy\":false,\"is_anonymous_vpn\":false,\"is_anycast\":false,\"is_hosting_provider\":false,\"is_legitimate_proxy\":false,\"is_public_proxy\":false,\"is_residential_proxy\":false,\"is_satellite_provider\":false,\"is_tor_exit_node\":false,\"network\":\"8.8.8.0/23\"}} ]")
    }

    @Test fun `it should resolve geolocation in session`() = runTest {
        val email = "test@example.com"
        val sessionId = "123456"
        val body = RegisterUserRequest(email = email, password = "password", "Name", SessionInfoRequest(id = sessionId))

        webTestClient.post()
            .uri("/api/auth/register")
            .header("X-Real-IP", "93.128.176.188")
            .bodyValue(body)
            .exchange()
            .expectStatus()
            .isOk

        val user = userService.findByEmail(email)
        val location = user.sensitive.sessions.firstOrNull { it.id == sessionId }?.location

        println(location)

        assertThat(location?.cityName).isEqualTo("Berlin")
        assertThat(location?.countryCode).isEqualTo("DE")
    }
}
