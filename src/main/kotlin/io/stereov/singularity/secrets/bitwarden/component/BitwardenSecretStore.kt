package io.stereov.singularity.secrets.bitwarden.component

import com.bitwarden.sdk.BitwardenClient
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.secrets.bitwarden.properties.BitwardenSecretStoreProperties
import io.stereov.singularity.secrets.core.component.SecretCache
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.core.exception.model.SecretKeyNotFoundException
import io.stereov.singularity.secrets.core.model.Secret
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.*

@Component
@ConditionalOnProperty(prefix = "singularity.secrets", value = ["store"], havingValue = "bitwarden", matchIfMissing = false)
class BitwardenSecretStore(
    private val bitwardenClient: BitwardenClient,
    private val properties: BitwardenSecretStoreProperties,
    private val cache: SecretCache
) : SecretStore {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun getSecretById(id: UUID): Secret = withContext(Dispatchers.IO) {
        logger.debug { "Getting secret by ID $id" }

        val cachedSecret = cache.get(id)
        if (cachedSecret != null) return@withContext cachedSecret

        val secretResponse = bitwardenClient.secrets().get(id)
        val secret =
            Secret(secretResponse.id, secretResponse.key, secretResponse.value, secretResponse.creationDate.toInstant())

        cache.put(secret)
        return@withContext secret
    }

    override suspend fun getOrNull(key: String): Secret? = withContext(Dispatchers.IO) {
        logger.debug { "Getting secret by key $key" }

        val loadedSecret = cache.getByKey(key)

        if (loadedSecret != null) return@withContext loadedSecret

        return@withContext bitwardenClient.secrets().list(properties.organizationId).data
            .firstOrNull { secret -> secret.key == key }
            ?.let { getSecretById(it.id) }
    }

    suspend fun create(key: String, value: String, note: String): Secret = withContext(Dispatchers.IO) {
        logger.debug { "Creating secret with key $key" }

        val secretResponse =
            bitwardenClient.secrets().create(properties.organizationId, key, value, note, arrayOf(properties.projectId))

        val id = secretResponse.id

        val secret = Secret(id, key, value, secretResponse.creationDate.toInstant())
        cache.put(secret)

        return@withContext secret
    }

    override suspend fun put(key: String, value: String, note: String): Secret =
        withContext(Dispatchers.IO) {
            logger.debug { "Creating or updating key $key" }

            get(key) ?: return@withContext create(key, value, note)

            val updatedSecret = update(key, value, note)
            cache.put(updatedSecret)

            return@withContext updatedSecret
        }

    suspend fun update(key: String, value: String, note: String): Secret =
        withContext(Dispatchers.IO) {
            val id = get(key)?.id ?: throw SecretKeyNotFoundException(key)

            val res = bitwardenClient.secrets()
                .update(properties.organizationId, id, key, value, note, arrayOf(properties.projectId))

            val updatedSecret = Secret(res.id, key, res.value, res.creationDate.toInstant())

            cache.put(updatedSecret)

            return@withContext updatedSecret
        }
}