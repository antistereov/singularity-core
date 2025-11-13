package io.stereov.singularity.database.encryption.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KLogger
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

@Service
class EncryptionService(
    private val encryptionSecretService: EncryptionSecretService,
    private val secretStore: SecretStore,
    private val objectMapper: ObjectMapper
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun <T> wrap(value: T): Result<Encrypted<T>, EncryptionException> = withContext(Dispatchers.IO) {
        return@withContext runCatching {
            objectMapper.writeValueAsString(value)
        }
            .mapError { ex -> EncryptionException.ObjectMapping("Failed to write object as string: ${ex.message}", ex) }
            .andThen { jsonStr -> encrypt(jsonStr) }
    }

    suspend fun <T> unwrap(encrypted: Encrypted<T>, clazz: Class<T>): Result<T, EncryptionException> = withContext(Dispatchers.IO) {
        return@withContext decrypt(encrypted).flatMap { decryptedJson ->
            runCatching { objectMapper.readValue(decryptedJson, clazz) }
                .mapError { ex -> EncryptionException.ObjectMapping("Failed to create object of string $decryptedJson: ${ex.message}", ex) }
        }
    }

    suspend fun <T> encrypt(strToEncrypt: String): Result<Encrypted<T>, EncryptionException> = coroutineBinding {
        logger.debug { "Encrypting..." }

        val secret = encryptionSecretService.getCurrentSecret()
            .mapError { ex -> EncryptionException.Secret("Failed to retrieve encryption secret", ex) }
            .bind()

        val secretEncryptionKey = getKeyFromBase64(secret.value)
            .bind()

        runCatching { Cipher.getInstance("AES/ECB/PKCS5Padding") }
            .mapError { ex -> EncryptionException.Cipher("Failed to get cipher instance: ${ex.message}", ex) }
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
            .mapError { ex -> EncryptionException.Cipher("Failed to get cipher instance: ${ex.message}", ex) }
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
