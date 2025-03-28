package io.stereov.web.global.service.encryption

import io.stereov.web.properties.EncryptionProperties
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
    private val properties: EncryptionProperties,
) {

    private val logger = LoggerFactory.getLogger(EncryptionService::class.java)

    fun encrypt(strToEncrypt: String): String {
        logger.debug("Encrypting...")

        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKeySpec = SecretKeySpec(properties.secretKey.toByteArray(), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec)
        val encrypted = cipher.doFinal(strToEncrypt.toByteArray())
        return Base64.getUrlEncoder().encodeToString(encrypted)
    }

    fun decrypt(strToDecrypt: String): String {
        logger.debug("Decrypting...")

        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKeySpec = SecretKeySpec(properties.secretKey.toByteArray(), "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec)
        val decrypted = cipher.doFinal(Base64.getUrlDecoder().decode(strToDecrypt))
        return String(decrypted)
    }

}
