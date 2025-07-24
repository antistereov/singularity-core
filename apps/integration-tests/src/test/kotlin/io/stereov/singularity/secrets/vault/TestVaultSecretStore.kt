package io.stereov.singularity.secrets.vault

import com.mongodb.assertions.Assertions.assertNull
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.core.exception.model.SecretKeyNotFoundException
import io.stereov.singularity.secrets.core.properties.SecretStoreImplementation
import io.stereov.singularity.secrets.vault.component.VaultSecretStore
import io.stereov.singularity.secrets.vault.properties.VaultSecretStoreProperties
import io.stereov.singularity.test.BaseSpringBootTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName
import org.testcontainers.vault.VaultContainer
import java.time.temporal.ChronoUnit
import java.util.*

class TestVaultSecretStore : BaseSpringBootTest() {

    @AfterEach
    fun clearDatabase() = runBlocking {
        userService.deleteAll()
    }

    @Autowired
    private lateinit var secretStore: VaultSecretStore

    @Test fun `should initialize beans`() {
        applicationContext.getBean(VaultSecretStoreProperties::class.java)
        val secretStore = applicationContext.getBean(SecretStore::class.java)

        assertThat(secretStore).isOfAnyClassIn(VaultSecretStore::class.java)
    }

    @Test fun `put and get works`() = runTest {
        val key = UUID.randomUUID().toString()
        val value = "value"

        val secret = secretStore.put(key, value)

        assertThat(secret.key).isEqualTo(key)
        assertThat(secret.value).isEqualTo(value)

        val savedSecret = secretStore.get(key)

        assertThat(savedSecret).isEqualTo(secret.copy(createdAt = savedSecret.createdAt.truncatedTo(ChronoUnit.MILLIS)))
    }
    @Test fun `get throws exception when no secret exists`() = runTest {
        assertThrows<SecretKeyNotFoundException> { secretStore.get("random-key") }
    }
    @Test fun `getOrNull return null when no secret exists`() = runTest {
        assertNull(secretStore.getOrNull("random-key"))
    }

    companion object {
        val mongoDBContainer = MongoDBContainer("mongo:latest").apply {
            start()
        }

        private val redisContainer = GenericContainer(DockerImageName.parse("redis:latest"))
            .withExposedPorts(6379)
            .apply {
                start()
            }

        private const val vaultToken = "test-token"

        private val vaultContainer = VaultContainer("hashicorp/vault:latest")
            .withVaultToken(vaultToken)
            .withExposedPorts(8200)
            .withInitCommand("secrets enable -path=apps -version=2 kv")
            .apply { start() }


        @DynamicPropertySource
        @JvmStatic
        @Suppress("UNUSED")
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.data.mongodb.uri") { "${mongoDBContainer.connectionString}/test" }
            registry.add("spring.data.redis.host") { redisContainer.host }
            registry.add("spring.data.redis.port") { redisContainer.getMappedPort(6379) }
            registry.add("singularity.security.rate-limit.user-limit") { 10000 }
            registry.add("singularity.secrets.store") { SecretStoreImplementation.VAULT }
            registry.add("singularity.secrets.vault.host") { vaultContainer.host }
            registry.add("singularity.secrets.vault.port") { vaultContainer.getMappedPort(8200) }
            registry.add("singularity.secrets.vault.token") { vaultToken }
        }
    }
}
