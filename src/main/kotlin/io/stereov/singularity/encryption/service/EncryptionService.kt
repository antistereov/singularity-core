package io.stereov.singularity.encryption.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.encryption.model.Encrypted
import io.stereov.singularity.global.database.model.EncryptedSensitiveDocument
import io.stereov.singularity.global.database.model.SensitiveDocument
import io.stereov.singularity.secrets.component.KeyManager
import io.stereov.singularity.secrets.service.EncryptionSecretService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@Service
class EncryptionService(
    private val encryptionSecretService: EncryptionSecretService,
    private val keyManager: KeyManager,
    private val objectMapper: ObjectMapper
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun <S, D: SensitiveDocument<S>> encrypt(
        document: D,
        otherValues: List<Any> = emptyList()
    ): EncryptedSensitiveDocument<S> {
        val wrapped = wrap(document.sensitive)

        return document.toEncryptedDocument(wrapped, otherValues)
    }

    suspend fun <S, E: EncryptedSensitiveDocument<S>> decrypt(
        encryptedDocument: E,
        otherValues: List<Any> = emptyList(),
        clazz: Class<S>
    ): SensitiveDocument<S> {
        val unwrapped = unwrap(encryptedDocument.sensitive, clazz)

        return encryptedDocument.toSensitiveDocument(unwrapped, otherValues)
    }

    private suspend fun <T> wrap(value: T): Encrypted<T> = withContext(Dispatchers.IO) {
        val jsonStr = objectMapper.writeValueAsString(value)
        encrypt(jsonStr)
    }

    private suspend fun <T> unwrap(encrypted: Encrypted<T>, clazz: Class<T>): T = withContext(Dispatchers.IO) {
        val decryptedJson = decrypt(encrypted)
        objectMapper.readValue(decryptedJson, clazz)
    }

    suspend fun <T> encrypt(strToEncrypt: String): Encrypted<T> {
        this.logger.debug { "Encrypting..." }

        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secret = encryptionSecretService.getCurrentSecret()

        cipher.init(Cipher.ENCRYPT_MODE, getKeyFromBase64(secret.value))
        val encrypted = cipher.doFinal(strToEncrypt.toByteArray())

        val encryptedString = Base64.getUrlEncoder().encodeToString(encrypted)

        return Encrypted(secret.id, encryptedString)
    }

    suspend fun <T> decrypt(encrypted: Encrypted<T>): String {
       this. logger.debug { "Decrypting..." }

        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secret = this.keyManager.getSecretById(encrypted.secretId)

        cipher.init(Cipher.DECRYPT_MODE, getKeyFromBase64(secret.value))

        val decrypted = cipher.doFinal(Base64.getUrlDecoder().decode(encrypted.ciphertext))

        return String(decrypted)
    }

    private fun getKeyFromBase64(base64Key: String, algorithm: String = "AES"): SecretKeySpec {
        val decodedKey = Base64.getDecoder().decode(base64Key)
        return SecretKeySpec(decodedKey, algorithm)
    }
}
