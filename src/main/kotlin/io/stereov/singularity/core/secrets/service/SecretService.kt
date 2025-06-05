package io.stereov.singularity.core.secrets.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.core.secrets.component.KeyManager
import io.stereov.singularity.core.secrets.model.Secret
import io.stereov.singularity.core.properties.AppProperties
import java.time.Instant
import java.util.*
import javax.crypto.KeyGenerator

abstract class SecretService(
    private val keyManager: KeyManager,
    key: String,
    private val algorithm: String,
    appProperties: AppProperties
) {

    protected val logger: KLogger = KotlinLogging.logger {}
    private val actualKey = "${appProperties.slug}-$key"
    private var currentSecret: Secret? = null

    suspend fun getCurrentSecret(): Secret {
        this.logger.debug { "Getting current secret" }

        return this.currentSecret
            ?: this.loadCurrentSecret()
    }

    private suspend fun loadCurrentSecret(): Secret {
        this.logger.debug { "Loading current secret from key manager" }

        val currentSecret = this.keyManager.getSecretByKey(actualKey)

        val secret = currentSecret?.let {
            this.keyManager.getSecretById(UUID.fromString(currentSecret.value))
        } ?: this.updateSecret()

        this.currentSecret = secret

        return secret
    }

    suspend fun updateSecret(): Secret {
        this.logger.debug { "Updating current secret" }

        val newKey = "$actualKey-${Instant.now()}"
        val newValue = this.generateKey(algorithm = algorithm)
        val newNote = "Generated on ${Instant.now()}"

        val newSecret = this.keyManager.create(newKey, newValue, newNote)
        this.currentSecret = newSecret

        this.keyManager.createOrUpdateKey(this.actualKey, newSecret.id.toString(), newNote)

        return newSecret
    }

    fun generateKey(keySize: Int = 256, algorithm: String = this.algorithm): String {
        val keyGenerator = KeyGenerator.getInstance(algorithm)
        keyGenerator.init(keySize)

        return Base64.getEncoder().encodeToString(keyGenerator.generateKey().encoded)
    }

    fun getLastUpdate(): Instant? = this.currentSecret?.createdAt
}
