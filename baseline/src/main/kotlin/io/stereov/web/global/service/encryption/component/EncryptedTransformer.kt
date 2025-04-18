package io.stereov.web.global.service.encryption.component

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.encryption.model.Encrypted
import io.stereov.web.global.service.encryption.model.EncryptedSensitiveDocument
import io.stereov.web.global.service.encryption.model.SensitiveDocument
import io.stereov.web.global.service.secrets.component.KeyManager
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

@Component
class EncryptedTransformer(
    private val keyManager: KeyManager,
    private val json: Json
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    fun <S, D: SensitiveDocument<S>> encrypt(
        document: D,
        serializer: KSerializer<S>,
        otherValues: List<Any> = emptyList()
    ): EncryptedSensitiveDocument<S> {
        val wrapped = wrap(document.sensitive, serializer)

        return document.toEncryptedDocument(wrapped, otherValues)
    }

    fun <S, E: EncryptedSensitiveDocument<S>> decrypt(
        encryptedDocument: E,
        serializer: KSerializer<S>,
        otherValues: List<Any> = emptyList(),
    ): SensitiveDocument<S> {
        val unwrapped = unwrap(encryptedDocument.sensitive, serializer)

        return encryptedDocument.toSensitiveDocument(unwrapped, otherValues)
    }

    private fun <T> wrap(value: T, serializer: KSerializer<T>): Encrypted<T> {
        val jsonStr = json.encodeToString(serializer, value)
        return this.encrypt(jsonStr)
    }

    private fun <T> unwrap(encrypted: Encrypted<T>, serializer: KSerializer<T>): T {
        val decryptedJson = this.decrypt(encrypted)
        return json.decodeFromString(serializer, decryptedJson)
    }

    private fun <T> encrypt(strToEncrypt: String): Encrypted<T> {
        this.logger.debug { "Encrypting..." }

        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secret = keyManager.getEncryptionSecret()

        cipher.init(Cipher.ENCRYPT_MODE, getKeyFromBase64(secret.value))
        val encrypted = cipher.doFinal(strToEncrypt.toByteArray())

        val encryptedString = Base64.getUrlEncoder().encodeToString(encrypted)

        return Encrypted(secret.id, encryptedString)
    }

    private fun <T> decrypt(encrypted: Encrypted<T>): String {
       this. logger.debug { "Decrypting..." }

        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secret = keyManager.getSecretById(encrypted.secretId)

        cipher.init(Cipher.DECRYPT_MODE, getKeyFromBase64(secret.value))

        val decrypted = cipher.doFinal(Base64.getUrlDecoder().decode(encrypted.ciphertext))

        return String(decrypted)
    }

    private fun getKeyFromBase64(base64Key: String, algorithm: String = "AES"): SecretKeySpec {
        val decodedKey = Base64.getDecoder().decode(base64Key)
        return SecretKeySpec(decodedKey, algorithm)
    }
}
