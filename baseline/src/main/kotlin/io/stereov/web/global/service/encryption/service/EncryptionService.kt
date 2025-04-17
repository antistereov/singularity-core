package io.stereov.web.global.service.encryption.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.encryption.model.EncryptedField
import io.stereov.web.global.service.secrets.component.KeyManager
import org.springframework.stereotype.Service
import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * # Service for encrypting and decrypting strings.
 *
 * This service uses the AES encryption algorithm in ECB mode with PKCS5 padding.
 * It provides methods to encrypt and decrypt strings using a secret key.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class EncryptionService(
    private val keyManager: KeyManager,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    fun encrypt(strToEncrypt: String): EncryptedField {
        logger.debug { "Encrypting..." }

        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secret = keyManager.getEncryptionSecret()

        cipher.init(Cipher.ENCRYPT_MODE, getKeyFromBase64(secret.value))
        val encrypted = cipher.doFinal(strToEncrypt.toByteArray())

        val encryptedString = Base64.getUrlEncoder().encodeToString(encrypted)

        return EncryptedField(secret.id, encryptedString)
    }

    fun decrypt(fieldToDecrypt: EncryptedField): String {
        logger.debug { "Decrypting..." }

        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secret = keyManager.getSecretById(fieldToDecrypt.keyId)

        cipher.init(Cipher.DECRYPT_MODE, getKeyFromBase64(secret.value))

        val decrypted = cipher.doFinal(Base64.getUrlDecoder().decode(fieldToDecrypt.data))

        return String(decrypted)
    }

    fun matches(input: String, encrypted: EncryptedField): Boolean {
        val decrypted = decrypt(encrypted)

        return input == decrypted
    }

    fun rotateKey(field: EncryptedField): EncryptedField {
        logger.debug { "Rotating key" }

        val newKey = this.keyManager.getEncryptionSecret()

        if (field.keyId == newKey.id) {
            logger.warn { "Encryption secret is the same. Skipping rotation." }
        }

        val decrypted = this.decrypt(field)

        return this.encrypt(decrypted)
    }

    private fun getKeyFromBase64(base64Key: String, algorithm: String = "AES"): SecretKeySpec {
        val decodedKey = Base64.getDecoder().decode(base64Key)
        return SecretKeySpec(decodedKey, algorithm)
    }

}
