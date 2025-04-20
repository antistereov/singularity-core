package io.stereov.web.global.service.secrets.component

import com.bitwarden.sdk.BitwardenClient
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.secrets.model.Secret
import io.stereov.web.properties.secrets.BitwardenKeyManagerProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.OffsetDateTime
import java.util.*

@Component
@ConditionalOnProperty(prefix = "baseline.secrets", value = ["key-manager"], havingValue = "bitwarden", matchIfMissing = false)
class BitwardenKeyManager(
    private val bitwardenClient: BitwardenClient,
    private val properties: BitwardenKeyManagerProperties,
) : KeyManager {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    private val loadedKeys = mutableSetOf<Secret>()

    override fun getSecretById(id: UUID): Secret {
        logger.debug { "Getting secret by ID $id" }

        val cachedSecret = this.loadedKeys.firstOrNull { it.id == id }
        if (cachedSecret != null) return cachedSecret

        this.bitwardenClient.secrets().sync(properties.organizationId, OffsetDateTime.now())
        val secretResponse = bitwardenClient.secrets().get(id)
        val secret = Secret(secretResponse.id, secretResponse.key, secretResponse.value, secretResponse.creationDate.toInstant())

        this.loadedKeys.add(secret)
        return secret
    }

    override fun getSecretByKey(key: String): Secret? {
        this.logger.debug { "Getting secret by key $key" }

        return this.loadedKeys
            .firstOrNull { it.key == key }
            ?: this.bitwardenClient.secrets().list(properties.organizationId).data
                .firstOrNull { secret -> secret.key == key }
                ?.let { getSecretById(it.id) }
    }

    override fun create(key: String, value: String, note: String): Secret {
        this.logger.debug { "Creating secret with key $key" }

        val secretResponse = bitwardenClient.secrets().create(properties.organizationId, key, value, note, arrayOf(properties.projectId))

        val id = secretResponse.id

        val secret =  Secret(id, key, value, secretResponse.creationDate.toInstant())
        loadedKeys.add(secret)

        return secret
    }

    override fun createOrUpdateKey(key: String, value: String, note: String): Secret {
        this.logger.debug { "Creating or updating key $key" }

        val existingSecret = getSecretByKey(key) ?: return this.create(key, value, note)

        val updatedSecret = update(existingSecret.id, key, value, note)
        loadedKeys.remove(existingSecret)
        loadedKeys.add(updatedSecret)

        return updatedSecret
    }

    override fun update(id: UUID, key: String, value: String, note: String): Secret {
        val res = this.bitwardenClient.secrets().update(properties.organizationId, id, key, value, note, arrayOf(properties.projectId))

        val updatedSecret = Secret(res.id, key, res.value, res.creationDate.toInstant())

        loadedKeys.removeAll { it.id == id }
        loadedKeys.add(updatedSecret)

        return updatedSecret
    }
}
