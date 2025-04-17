package io.stereov.web.global.service.secrets.component

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.secrets.exception.model.NoCurrentEncryptionKeyException
import io.stereov.web.global.service.secrets.exception.model.SecretKeyNotFoundException
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
class EnvKeyManager {

    private val keys = mutableMapOf<String, String>()
    private val logger: KLogger
        get() = KotlinLogging.logger {}

    @PostConstruct
    fun initializeKeysFromEnv() {
        logger.debug { "Initializing secret keys from environment variables" }

        System.getenv().forEach { (id, _) ->
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
        logger.debug { "Getting ID of current encryption key" }

        return System.getenv("CURRENT_SECRET_KEY")
            ?: throw NoCurrentEncryptionKeyException()
    }

    fun getKeyById(keyId: String): String {
        logger.debug { "Getting encryption key by ID $keyId" }

        return keys[keyId]
            ?: throw SecretKeyNotFoundException(keyId)
    }
}
