package io.stereov.singularity.secrets.local.component

import io.stereov.singularity.secrets.core.component.SecretCache
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.core.model.Secret
import io.stereov.singularity.secrets.local.data.LocalSecretEntity
import io.stereov.singularity.secrets.local.repository.LocalSecretRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
@Primary
@ConditionalOnProperty(prefix = "singularity.secrets", value = ["store"], havingValue = "local", matchIfMissing = true)
class LocalSecretStore(
    private val repository: LocalSecretRepository,
    override val secretCache: SecretCache
) : SecretStore {

    override suspend fun doGetOrNull(key: String): Secret? {
        return repository.findByKey(key)?.toSecret()
    }

    override suspend fun doPut(
        key: String,
        value: String,
        note: String,
    ): Secret {
        val existingSecret = repository.findByKey(key)
        val uuid = existingSecret?.id ?: UUID.randomUUID()
        val newSecret = LocalSecretEntity(
            id = uuid.toString(),
            key = key,
            value = value,
            createdAt = existingSecret?.createdAt ?: Instant.now()
        )

        return repository.put(newSecret).toSecret()
    }
}
