package io.stereov.singularity.secrets.local.component

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.andThen
import com.github.michaelbull.result.map
import com.github.michaelbull.result.recoverIf
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.secrets.core.component.SecretCache
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.core.exception.SecretStoreException
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

    override val logger = KotlinLogging.logger {}

    override suspend fun doGet(key: String): Result<Secret, SecretStoreException> {
        return repository.findByKey(key).map { it.toSecret() }
    }

    override suspend fun doPut(
        key: String,
        value: String,
        note: String,
    ): Result<Secret, SecretStoreException> {
        return repository.findByKey(key)
            .recoverIf(
                { ex -> ex is SecretStoreException.NotFound },
                { LocalSecretEntity(
                    id = UUID.randomUUID().toString(),
                    key = key,
                    value = value,
                    createdAt = Instant.now()
                ) }
            )
            .andThen { entity ->
                val newSecret = LocalSecretEntity(
                    id = entity.toString(),
                    key = key,
                    value = value,
                    createdAt = Instant.now()
                )

                repository.put(newSecret)
            }
            .map { it.toSecret() }
    }
}
