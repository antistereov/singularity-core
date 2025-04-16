package io.stereov.web.global.service.encryption.component

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.encryption.exception.model.NoSecurityKeySetException
import io.stereov.web.global.service.encryption.exception.model.SecretKeyNotFoundException
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
class KeyManager {

    private val keys = mutableMapOf<String, String>()
    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @PostConstruct
    fun initializeKeysFromEnv() {
        System.getenv().forEach { (id, value) ->
            if (id.startsWith("SECRET_KEY_ID_")) {
                val parts = id.split("_")
                if (parts.size == 4) {
                    val index = parts[3]
                    val idKey = "SECRET_KEY_ID_$index"
                    val valueKey = "SECRET_KEY_VALUE_$index"

                    val keyId = System.getenv(idKey)
                    val secret = System.getenv(valueKey)

                    if (!keyId.isNullOrBlank() && !secret.isNullOrBlank()) {
                        keys[keyId] = secret
                    }
                }
            }
        }
    }

    fun getCurrentKeyId(): String {
        return System.getenv("CURRENT_SECRET_KEY")
            ?: throw NoSecurityKeySetException()
    }

    fun getKeyById(keyId: String): String {
        return keys[keyId]
            ?: throw SecretKeyNotFoundException(keyId)
    }
}
