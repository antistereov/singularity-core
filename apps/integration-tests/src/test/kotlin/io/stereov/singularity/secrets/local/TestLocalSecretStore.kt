package io.stereov.singularity.secrets.local

import com.github.michaelbull.result.getOrThrow
import com.mongodb.assertions.Assertions.assertNull
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.core.exception.SecretStoreException
import io.stereov.singularity.secrets.local.component.LocalSecretStore
import io.stereov.singularity.secrets.local.properties.LocalSecretStoreProperties
import io.stereov.singularity.test.BaseIntegrationTest
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.time.temporal.ChronoUnit
import java.util.*

class TestLocalSecretStore : BaseIntegrationTest() {

    @Autowired
    private lateinit var secretStore: LocalSecretStore

    @Test fun `should initialize beans`() {
        applicationContext.getBean(LocalSecretStoreProperties::class.java)
        val secretStore = applicationContext.getBean(SecretStore::class.java)

        assertThat(secretStore).isOfAnyClassIn(LocalSecretStore::class.java)
    }

    @Test fun `put and get works`() = runTest {
        val key = UUID.randomUUID().toString()
        val value = "value"

        val secret = secretStore.put(key, value).getOrThrow()

        assertThat(secret.key).isEqualTo(key)
        assertThat(secret.value).isEqualTo(value)

        val savedSecret = secretStore.get(key).getOrThrow()

        assertThat(savedSecret.copy(createdAt = savedSecret.createdAt.truncatedTo(ChronoUnit.MILLIS))).isEqualTo(secret.copy(createdAt = savedSecret.createdAt.truncatedTo(ChronoUnit.MILLIS)))
    }
    @Test fun `get throws exception when no secret exists`() = runTest {
        assertThrows<SecretStoreException.NotFound> { secretStore.get("random-key").getOrThrow() }
    }
    @Test fun `getOrNull return null when no secret exists`() = runTest {
        assertNull(secretStore.get("random-key"))
    }
}
