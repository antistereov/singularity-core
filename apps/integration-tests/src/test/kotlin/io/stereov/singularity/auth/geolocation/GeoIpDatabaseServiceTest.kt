package io.stereov.singularity.auth.geolocation

import io.stereov.singularity.auth.geolocation.service.GeoIpDatabaseService
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.net.InetAddress
import kotlin.io.path.Path

class GeoIpDatabaseServiceTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var geoIpDatabaseService: GeoIpDatabaseService

    companion object {
        val location = Path("./.data/test/files/geolocation")
        const val FILENAME = "GeoLite2-City.mmdb"
        val file = location.toFile().resolve(FILENAME)

        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("singularity.auth.geolocation.enabled") { true }
            registry.add("singularity.auth.geolocation.download") { true }
            registry.add("singularity.auth.geolocation.account-id") { System.getenv("MAXMIND_ACCOUNT_ID") }
            registry.add("singularity.auth.geolocation.license-key") { System.getenv("MAXMIND_LICENSE_KEY") }
            registry.add("singularity.auth.geolocation.database-directory") { location.toString() }
            registry.add("singularity.auth.geolocation.database-filename") { FILENAME }
        }
    }

    @Test fun `should download database on initialize`() = runTest {
        assertThat(file.exists())

        val location = geoIpDatabaseService.getCity(InetAddress.getByName("93.128.176.188"))

        println(location)

        assertThat(location.city.names["en"]).isEqualTo("Berlin")
        assertThat(location.country.isoCode).isEqualTo("DE")
    }

    @Test @DirtiesContext fun `should download database`() = runTest {
        file.delete()

        geoIpDatabaseService.initialize()

        val location = geoIpDatabaseService.getCity(InetAddress.getByName("93.128.176.188"))

        println(location)

        assertThat(location.city.names["en"]).isEqualTo("Berlin")
        assertThat(location.country.isoCode).isEqualTo("DE")

        file.delete()
    }

    @Test @DirtiesContext fun `should update database on outdated`() = runTest {
        file.delete()

        file.createNewFile()
        file.setLastModified(0)

        geoIpDatabaseService.initialize()

        val location = geoIpDatabaseService.getCity(InetAddress.getByName("93.128.176.188"))

        println(location)

        assertThat(location.city.names["en"]).isEqualTo("Berlin")
        assertThat(location.country.isoCode).isEqualTo("DE")

        file.delete()
    }
}
