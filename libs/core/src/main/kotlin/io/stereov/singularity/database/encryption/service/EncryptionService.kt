package io.stereov.singularity.database.encryption.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.database.encryption.exception.EncryptionException
import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.secrets.core.component.SecretStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Service class responsible for handling encryption and decryption operations.
 *
 * This service provides methods for wrapping and unwrapping objects with encryption, as well as
 * performing encryption and decryption of string data. It relies on an encryption secret service
 * to retrieve keys, a secret store for key management, and an object mapper to serialize and
 * deserialize objects.
 */
@Service
class EncryptionService(
    private val encryptionSecretService: EncryptionSecretService,
    private val secretStore: SecretStore,
    private val objectMapper: ObjectMapper
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Encrypts the provided value and wraps it in the [Encrypted] data structure.
     *
     * This method serializes the input value into a JSON string, encrypts it, and returns
     * the result wrapped in a [Result] object containing the encrypted data or an error.
     *
     * @param T The type of the value to be encrypted.
     * @param value The value to be serialized, encrypted, and wrapped.
     * @return A [Result] containing either the encrypted data as [Encrypted] on success,
     *   or an [EncryptionException] on failure.
     */
    suspend fun <T> wrap(value: T): Result<Encrypted<T>, EncryptionException> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            objectMapper.writeValueAsString(value)
        }
            .mapError { ex -> EncryptionException.ObjectMapping("Failed to write object as string: ${ex.message}", ex) }
            .andThen { jsonStr -> encrypt(jsonStr) }
    }

    /**
     * Decrypts the given encrypted data and deserializes it into an object of the specified class type.
     *
     * This method performs the decryption of the encrypted content using the provided secret key
     * and then deserializes the decrypted JSON string into an object of the provided class type.
     *
     * @param T The type of the object to be deserialized.
     * @param encrypted The encrypted data to be decrypted and deserialized.
     * @param clazz The target class type to which the decrypted JSON string is deserialized.
     * @return A [Result] containing the deserialized object of type [T] on success,
     *   or an [EncryptionException] on failure.
     */
    suspend fun <T> unwrap(encrypted: Encrypted<T>, clazz: Class<T>): Result<T, EncryptionException> = withContext(Dispatchers.IO) {
        return@withContext decrypt(encrypted).flatMap { decryptedJson ->
            runCatching { objectMapper.readValue(decryptedJson, clazz) }
                .mapError { ex -> EncryptionException.ObjectMapping("Failed to create object of string $decryptedJson: ${ex.message}", ex) }
        }
    }

    /**
     * Encrypts the provided string using the current encryption secret and returns the result.
     * This method utilizes AES encryption with ECB mode and PKCS5 padding.
     *
     * @param strToEncrypt the plain text string to be encrypted
     * @return a [Result] object containing the encrypted string wrapped in an [Encrypted] object on success,
     *  or an [EncryptionException] on failure
     */
    suspend fun <T> encrypt(strToEncrypt: String): Result<Encrypted<T>, EncryptionException> = coroutineBinding {
        logger.debug { "Encrypting..." }

        val secret = encryptionSecretService.getCurrentSecret()
            .mapError { ex -> EncryptionException.Secret("Failed to retrieve encryption secret", ex) }
            .bind()

        val secretEncryptionKey = getKeyFromBase64(secret.value)
            .bind()

        runCatching { Cipher.getInstance("AES/ECB/PKCS5Padding") }
            .mapError { ex -> EncryptionException.Cipher("Failed to generate cipher instance: ${ex.message}", ex) }
            .flatMap { cipher ->
                runCatching { cipher.init(Cipher.ENCRYPT_MODE, secretEncryptionKey) }
                    .mapError { ex -> EncryptionException.Cipher("Failed to initialize cipher instance: ${ex.message}", ex) }
                    .map { cipher }
            }
            .flatMap { cipher ->
                runCatching { cipher.doFinal(strToEncrypt.toByteArray()) }
                    .mapError { ex -> EncryptionException.Cipher("Failed to encrypt string $strToEncrypt: ${ex.message}", ex) }
            }
            .flatMap { encrypted ->
                runCatching { Base64.getUrlEncoder().encodeToString(encrypted) }
                    .mapError { ex -> EncryptionException.Encoding("Failed to encode encrypted string as Base64: ${ex.message}", ex) }
            }
            .map { encryptedString -> Encrypted<T>(secret.key, encryptedString) }
            .bind()
    }

    /**
     * Decrypts the given encrypted data and returns the original plaintext string.
     *
     * This method utilizes a specific encryption mechanism to decrypt the provided encrypted input.
     * If the decryption process fails at any step, an appropriate [EncryptionException] is returned encapsulated in a [Result].
     *
     * @param encrypted Represents the encrypted input data to be decrypted, containing the ciphertext and secret key reference.
     * @return A [Result] containing the decrypted plaintext string if successful, or an [EncryptionException] if an error occurred during decryption.
     */
    suspend fun <T> decrypt(encrypted: Encrypted<T>): Result<String, EncryptionException> = coroutineBinding {
        logger.debug { "Decrypting..." }

        val secret = secretStore.get(encrypted.secretKey)
            .mapError { ex ->
                EncryptionException.Secret(
                    "Failed to retrieve encryption key ${encrypted.secretKey}: ${ex.message}",
                    ex
                )
            }
            .bind()

        val decodedStr = runCatching { Base64.getUrlDecoder().decode(encrypted.ciphertext) }
            .mapError { ex ->
                EncryptionException.Encoding(
                    "Failed to decode encrypted string as Base64: ${ex.message}",
                    ex
                )
            }
            .bind()

        val secretEncryptionKey = getKeyFromBase64(secret.value)
            .bind()

        runCatching { Cipher.getInstance("AES/ECB/PKCS5Padding") }
            .mapError { ex -> EncryptionException.Cipher("Failed to generate cipher instance: ${ex.message}", ex) }
            .flatMap { cipher ->
                runCatching { cipher.init(Cipher.DECRYPT_MODE, secretEncryptionKey) }
                    .mapError { ex ->
                        EncryptionException.Cipher(
                            "Failed to initialize cipher instance: ${ex.message}",
                            ex
                        )
                    }
                    .map { cipher }
            }
            .flatMap { cipher ->
                runCatching { cipher.doFinal(decodedStr) }
                    .mapError { ex -> EncryptionException.Cipher("Failed to decrypt string: ${ex.message}", ex) }
                    .map { bytes -> String(bytes) }
            }
            .bind()
    }

    private fun getKeyFromBase64(base64Key: String, algorithm: String = "AES"): Result<SecretKeySpec, EncryptionException> {
        return runCatching {
            val decodedKey = Base64.getDecoder().decode(base64Key)
            SecretKeySpec(decodedKey, algorithm)
        }.mapError { ex -> EncryptionException.Cipher("Failed to decode cipher key: ${ex.message}", ex) }
    }
}
