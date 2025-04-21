package io.stereov.web.test.config

import io.stereov.web.global.service.secrets.component.KeyManager
import io.stereov.web.global.service.secrets.model.Secret
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class MockKeyManager : KeyManager {
    private val secrets: MutableList<Secret> = mutableListOf()

    override suspend fun getSecretByKey(key: String): Secret? {
        return secrets.firstOrNull { it.key == key }
    }

    override suspend fun getSecretById(id: UUID): Secret {
        return secrets.first { it.id == id }
    }

    override suspend fun create(key: String, value: String, note: String): Secret {
        val secret = Secret(UUID.randomUUID(), key, value, Instant.now())
        this.secrets.add(secret)

        return secret
    }

    override suspend fun createOrUpdateKey(key: String, value: String, note: String): Secret {
        val secret = Secret(UUID.randomUUID(), key, value, Instant.now())
        this.secrets.removeAll { it.key == key }
        this.secrets.add(secret)

        return secret
    }

    override suspend fun update(id: UUID, key: String, value: String, note: String): Secret {
        val secret = Secret(id, key, value, Instant.now())
        this.secrets.removeAll { it.id == id }
        this.secrets.add(secret)

        return secret
    }
}
