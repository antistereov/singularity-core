package io.stereov.singularity.secrets.core.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.core.exception.SecretStoreException
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

    suspend fun getCurrentSecret(): Result<Secret, SecretStoreException> {
        this.logger.trace { "Getting current secret" }

        return this.currentSecret
            ?.let { Ok(it) }
            ?: this.loadCurrentSecret()
    }

    private suspend fun loadCurrentSecret(): Result<Secret, SecretStoreException> {
        this.logger.trace { "Loading current secret from key manager" }

        return this.secretStore.get(actualKey)
            .andThenRecoverIf(
                { ex -> ex is SecretStoreException.NotFound },
                { updateSecret() }
            )
            .map { current ->
                this.currentSecret = current
                current
            }
    }

    suspend fun updateSecret(): Result<Secret, SecretStoreException> = coroutineBinding {
        logger.trace { "Updating current secret" }

        val newKey = "$actualKey-${Instant.now()}"
        val newValue = generateKey(algorithm = algorithm).bind()
        val newNote = "Generated on ${Instant.now()}"

        val newSecret = secretStore.put(newKey, newValue, newNote).bind()
        currentSecret = newSecret

        secretStore.put(actualKey, newSecret.key, newNote).bind()

        newSecret
    }

    suspend fun rotateSecret(): Result<Secret, SecretStoreException> {
        if (fixSecret) return getCurrentSecret()

        return updateSecret()
    }

    fun generateKey(keySize: Int = 256, algorithm: String = this.algorithm): Result<String, SecretStoreException> {
        return runCatching {
            val keyGenerator = KeyGenerator.getInstance(algorithm)
            keyGenerator.init(keySize)

            Base64.getEncoder().encodeToString(keyGenerator.generateKey().encoded)
        }.mapError { ex -> SecretStoreException.KeyGenerator("Failed to generate secret key: ${ex.message}", ex) }
    }

    fun getLastUpdate(): Instant? = this.currentSecret?.createdAt
}
