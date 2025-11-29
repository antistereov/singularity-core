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

/**
 * A service for managing and handling secrets securely.
 * It interacts with a `SecretStore` for storing and retrieving secrets and provides functionality for secret generation,
 * rotation, and management.
 *
 * @property logger A logger used for tracing and logging operations within the service.
 * @constructor Initializes the SecretService with the provided configurations.
 *
 * @param secretStore The store responsible for managing secrets.
 * @param key The key used to identify the secret in the store.
 * @param algorithm The cryptographic algorithm used to generate secrets.
 * @param appProperties The application properties containing configuration values.
 * @param fixSecret Flag indicating whether the current secret should be fixed and not rotated.
 */
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

    /**
     * Retrieves the current secret from memory if available, or loads it from the secret store.
     *
     * If the secret is already cached in memory (`currentSecret`), it is returned.
     * Otherwise, it triggers loading of the current secret by calling `loadCurrentSecret`.
     *
     * @return A [Result] containing the [Secret] if successful, or a [SecretStoreException] in case of failure.
     */
    suspend fun getCurrentSecret(): Result<Secret, SecretStoreException> {
        this.logger.trace { "Getting current secret" }

        return this.currentSecret
            ?.let { Ok(it) }
            ?: this.loadCurrentSecret()
    }

    /**
     * Loads the current secret from the secret store and updates the in-memory cache.
     *
     * This method attempts to retrieve the secret associated with the actual key from the secret store.
     * If the secret is not found (`SecretStoreException.UserNotFound`), it triggers the creation of a new secret
     * by calling `updateSecret`. Once successfully obtained, the secret is cached in memory.
     *
     * @return A [Result] containing the [Secret] if successful, or a [SecretStoreException] in case of failure.
     */
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

    /**
     * Updates the current secret by generating a new secret key and value, saving it in the secret store,
     * and updating the key mapping for the actual secret key. The new secret is then cached in memory.
     *
     * @return A [Result] containing the newly created [Secret] if successful, or a [SecretStoreException]
     * in case of failure during key generation, secret storage, or related operations.
     */
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

    /**
     * Attempts to rotate the current secret by either retrieving the existing secret or creating a new one.
     *
     * If `fixSecret` is true, it retrieves the current secret via `getCurrentSecret`.
     * Otherwise, it updates the secret by invoking `updateSecret`. This may involve
     * generating a new key, storing it in the secret store, and updating the cached secret.
     *
     * @return A [Result] containing the current or newly created [Secret] if successful,
     * or a [SecretStoreException] in case of failure during retrieval or updating.
     */
    suspend fun rotateSecret(): Result<Secret, SecretStoreException> {
        if (fixSecret) return getCurrentSecret()

        return updateSecret()
    }

    /**
     * Generates a cryptographic key encoded in Base64 format.
     *
     * @param keySize The size of the key in bits. Defaults to 256.
     * @param algorithm The algorithm used to generate the key. Defaults to the algorithm defined in the class context.
     * @return A [Result] containing the generated key as a [String] if successful, or a [SecretStoreException]
     * encapsulating the error in case of failure.
     */
    fun generateKey(keySize: Int = 256, algorithm: String = this.algorithm): Result<String, SecretStoreException> {
        return runCatching {
            val keyGenerator = KeyGenerator.getInstance(algorithm)
            keyGenerator.init(keySize)

            Base64.getEncoder().encodeToString(keyGenerator.generateKey().encoded)
        }.mapError { ex -> SecretStoreException.KeyGenerator("Failed to generate secret key: ${ex.message}", ex) }
    }

    /**
     * Retrieves the timestamp of the last update to the current secret.
     *
     * @return The [Instant] representing when the current secret was last updated,
     * or null if no update has been recorded.
     */
    fun getLastUpdate(): Instant? = this.currentSecret?.createdAt
}
