package io.stereov.singularity.test.config

import io.stereov.singularity.secrets.core.component.KeyManager
import io.stereov.singularity.secrets.core.model.Secret
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class MockKeyManager : KeyManager {
    private val secrets: MutableList<Secret> = mutableListOf()

    override suspend fun getOrNull(key: String): Secret? {
        return secrets.firstOrNull { it.key == key }
    }

    override suspend fun put(key: String, value: String, note: String): Secret {
        val secret = Secret(UUID.randomUUID(), key, value, Instant.now())
        this.secrets.removeAll { it.key == key }
        this.secrets.add(secret)

        return secret
    }
}
