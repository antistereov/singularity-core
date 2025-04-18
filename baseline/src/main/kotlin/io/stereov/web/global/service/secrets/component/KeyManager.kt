package io.stereov.web.global.service.secrets.component

import io.stereov.web.global.service.secrets.model.Secret
import java.util.*
import javax.crypto.KeyGenerator

interface KeyManager {

    fun getSecretById(id: UUID): Secret

    fun getEncryptionSecret(): Secret
    fun updateEncryptionSecret(): Secret

    fun getJwtSecret(): Secret
    fun updateJwtSecret(): Secret

    fun generateKey(keySize: Int = 256, algorithm: String = "AES"): String {
        val keyGenerator = KeyGenerator.getInstance(algorithm)
        keyGenerator.init(keySize)

        return Base64.getEncoder().encodeToString(keyGenerator.generateKey().encoded)
    }
}
