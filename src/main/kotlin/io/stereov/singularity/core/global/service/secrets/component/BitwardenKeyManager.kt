package io.stereov.singularity.core.global.service.secrets.component

import com.bitwarden.sdk.BitwardenClient
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.core.global.service.secrets.model.Secret
import io.stereov.singularity.core.properties.secrets.BitwardenKeyManagerProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.*

@Component
@ConditionalOnProperty(prefix = "baseline.secrets", value = ["key-manager"], havingValue = "bitwarden", matchIfMissing = false)
class BitwardenKeyManager(
    private val bitwardenClient: BitwardenClient,
    private val properties: BitwardenKeyManagerProperties,
    private val cache: SecretCache
) : KeyManager {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    override suspend fun getSecretById(id: UUID): Secret = withContext(Dispatchers.IO) {
        logger.debug { "Getting secret by ID $id" }

        val cachedSecret = cache.get(id)
        if (cachedSecret != null) return@withContext cachedSecret

        val secretResponse = bitwardenClient.secrets().get(id)
        val secret = Secret(secretResponse.id, secretResponse.key, secretResponse.value, secretResponse.creationDate.toInstant())

        cache.put(secret)
        return@withContext secret
    }

    override suspend fun getSecretByKey(key: String): Secret? = withContext(Dispatchers.IO) {
        logger.debug { "Getting secret by key $key" }

        val loadedSecret = cache.getByKey(key)

        if (loadedSecret != null) return@withContext loadedSecret

        return@withContext bitwardenClient.secrets().list(properties.organizationId).data
            .firstOrNull { secret -> secret.key == key }
            ?.let { getSecretById(it.id) }
    }

    override suspend fun create(key: String, value: String, note: String): Secret = withContext(Dispatchers.IO) {
        logger.debug { "Creating secret with key $key" }

        val secretResponse = bitwardenClient.secrets().create(properties.organizationId, key, value, note, arrayOf(properties.projectId))

        val id = secretResponse.id

        val secret =  Secret(id, key, value, secretResponse.creationDate.toInstant())
        cache.put(secret)

        return@withContext secret
    }

    override suspend fun createOrUpdateKey(key: String, value: String, note: String): Secret = withContext(Dispatchers.IO) {
        logger.debug { "Creating or updating key $key" }

        val existingSecret = getSecretByKey(key) ?: return@withContext create(key, value, note)

        val updatedSecret = update(existingSecret.id, key, value, note)
        cache.put(updatedSecret)

        return@withContext updatedSecret
    }

    override suspend fun update(id: UUID, key: String, value: String, note: String): Secret = withContext(Dispatchers.IO) {
        val res = bitwardenClient.secrets().update(properties.organizationId, id, key, value, note, arrayOf(properties.projectId))

        val updatedSecret = Secret(res.id, key, res.value, res.creationDate.toInstant())

        cache.put(updatedSecret)

        return@withContext updatedSecret
    }
}
