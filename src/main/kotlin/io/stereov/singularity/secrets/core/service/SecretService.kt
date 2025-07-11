package io.stereov.singularity.secrets.core.service

import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.core.model.Secret
import java.time.Instant
import java.util.*
import javax.crypto.KeyGenerator

abstract class SecretService(
    private val secretStore: SecretStore,
    key: String,
    private val algorithm: String,
    appProperties: AppProperties,
    private val fixSecret: Boolean = false,
) {

    abstract val logger: KLogger
    private val actualKey = "${appProperties.slug}-$key"
    private var currentSecret: Secret? = null

    suspend fun getCurrentSecret(): Secret {
        this.logger.debug { "Getting current secret" }

        return this.currentSecret
            ?: this.loadCurrentSecret()
    }

    private suspend fun loadCurrentSecret(): Secret {
        this.logger.debug { "Loading current secret from key manager" }

        val currentSecret = this.secretStore.getOrNull(actualKey)

        val secret = currentSecret?.let {
            this.secretStore.get(currentSecret.value)
        } ?: this.updateSecret()

        this.currentSecret = secret

        return secret
    }

    suspend fun updateSecret(): Secret {
        this.logger.debug { "Updating current secret" }

        val newKey = "$actualKey-${Instant.now()}"
        val newValue = this.generateKey(algorithm = algorithm)
        val newNote = "Generated on ${Instant.now()}"

        val newSecret = this.secretStore.put(newKey, newValue, newNote)
        this.currentSecret = newSecret

        this.secretStore.put(this.actualKey, newSecret.key, newNote)

        return newSecret
    }

    suspend fun rotateSecret(): Secret {
        if (fixSecret) return getCurrentSecret()

        return updateSecret()
    }

    fun generateKey(keySize: Int = 256, algorithm: String = this.algorithm): String {
        val keyGenerator = KeyGenerator.getInstance(algorithm)
        keyGenerator.init(keySize)

        return Base64.getEncoder().encodeToString(keyGenerator.generateKey().encoded)
    }

    fun getLastUpdate(): Instant? = this.currentSecret?.createdAt
}
