package io.stereov.web.global.service.encryption

import io.stereov.web.global.service.encryption.component.KeyManager
import io.stereov.web.global.service.encryption.model.EncryptedField
import org.slf4j.LoggerFactory
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

    private val logger = LoggerFactory.getLogger(EncryptionService::class.java)

    fun encrypt(strToEncrypt: String): EncryptedField {
        logger.debug("Encrypting...")

        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKeyId = keyManager.getCurrentKeyId()
        val secretKey = keyManager.getKeyById(secretKeyId)

        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
        val encrypted = cipher.doFinal(strToEncrypt.toByteArray())

        val encryptedString = Base64.getUrlEncoder().encodeToString(encrypted)

        return EncryptedField(secretKeyId, encryptedString)
    }

    fun decrypt(fieldToDecrypt: EncryptedField): String {
        logger.debug("Decrypting...")

        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKey = keyManager.getKeyById(fieldToDecrypt.keyId)

        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
        val decrypted = cipher.doFinal(Base64.getUrlDecoder().decode(fieldToDecrypt.data))

        return String(decrypted)
    }

    fun matches(input: String, encrypted: EncryptedField): Boolean {
        val decrypted = decrypt(encrypted)

        return input == decrypted
    }

}
