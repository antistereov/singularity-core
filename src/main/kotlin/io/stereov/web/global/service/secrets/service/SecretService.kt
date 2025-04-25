package io.stereov.web.global.service.secrets.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.secrets.component.KeyManager
import io.stereov.web.global.service.secrets.model.Secret
import java.time.Instant
import java.util.*
import javax.crypto.KeyGenerator

abstract class SecretService(
    private val keyManager: KeyManager,
    private val key: String,
    private val algorithm: String
) {

    protected val logger: KLogger
        get() = KotlinLogging.logger {}

    private var currentSecret: Secret? = null

    suspend fun getCurrentSecret(): Secret {
        this.logger.debug { "Getting current secret" }

        return this.currentSecret
            ?: this.loadCurrentSecret()
    }

    private suspend fun loadCurrentSecret(): Secret {
        this.logger.debug { "Loading current secret from key manager" }

        val currentSecret = this.keyManager.getSecretByKey(key)

        val secret = currentSecret?.let {
            this.keyManager.getSecretById(UUID.fromString(currentSecret.value))
        } ?: this.updateSecret()

        this.currentSecret = secret

        return secret
    }

    suspend fun updateSecret(): Secret {
        this.logger.debug { "Updating current secret" }

        val newKey = "$key-${Instant.now()}"
        val newValue = this.generateKey(algorithm = algorithm)
        val newNote = "Generated on ${Instant.now()}"

        val newSecret = this.keyManager.create(newKey, newValue, newNote)
        this.currentSecret = newSecret

        this.keyManager.createOrUpdateKey(this.key, newSecret.id.toString(), newNote)

        return newSecret
    }

    fun generateKey(keySize: Int = 256, algorithm: String = this.algorithm): String {
        val keyGenerator = KeyGenerator.getInstance(algorithm)
        keyGenerator.init(keySize)

        return Base64.getEncoder().encodeToString(keyGenerator.generateKey().encoded)
    }

    fun getLastUpdate(): Instant? = this.currentSecret?.createdAt
}
