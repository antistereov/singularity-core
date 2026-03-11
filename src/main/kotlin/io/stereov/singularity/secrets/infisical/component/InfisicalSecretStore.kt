package io.stereov.singularity.secrets.infisical.component

import com.github.michaelbull.result.*
import com.infisical.sdk.InfisicalSdk
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.secrets.core.component.SecretCache
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.core.exception.SecretStoreException
import io.stereov.singularity.secrets.core.model.Secret
import io.stereov.singularity.secrets.infisical.properties.InfisicalSecretStoreProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper
import java.time.Instant
import java.util.*

@Component
@ConditionalOnProperty(prefix = "singularity.secrets", value = ["store"], havingValue = "infisical", matchIfMissing = false)
class InfisicalSecretStore(
    private val infisicalProperties: InfisicalSecretStoreProperties,
    override val secretCache: SecretCache,
    private val infisical: InfisicalSdk,
    private val jsonMapper: JsonMapper
) : SecretStore {

    override val logger = KotlinLogging.logger {}

    override suspend fun doGet(key: String): Result<Secret, SecretStoreException> {
        return runCatching {
            infisical.Secrets().GetSecret(
                key,
                infisicalProperties.projectId,
                infisicalProperties.environmentSlug,
                infisicalProperties.secretPath,
                null,
                null,
                null
            )
        }
            .mapError { ex ->
                if (ex.message?.contains("code=404") == true && ex.message?.contains("Secret with name '$key' not found") == true) {
                    SecretStoreException.NotFound("No secret with key $key found in vault")
                } else {
                    SecretStoreException.Operation("Failed to generate secret with key $key from vault: ${ex.message}", ex) }
                }
            .flatMap {
                runCatching {
                    jsonMapper.readValue(it.secretValue, Secret::class.java)
                }.mapError { ex -> SecretStoreException.Operation("Failed to deserialize secret with key '$key': ${ex.message}", ex) }
            }
    }


    override suspend fun doPut(
        key: String,
        value: String,
        note: String
    ): Result<Secret, SecretStoreException> {
        val secret = Secret(UUID.randomUUID(), key, value, Instant.now())

        return runCatching {
            infisical.Secrets().CreateSecret(
                key,
                jsonMapper.writeValueAsString(secret),
                infisicalProperties.projectId,
                infisicalProperties.environmentSlug,
                infisicalProperties.secretPath,
            )
        }
            .recoverIf(
                { ex -> ex.message?.contains("code=400") == true && ex.message?.contains("Secret already exists") == true },
                {
                    infisical.Secrets().DeleteSecret(
                        key,
                        infisicalProperties.projectId,
                        infisicalProperties.environmentSlug,
                        infisicalProperties.secretPath
                    )
                    infisical.Secrets().CreateSecret(
                        key,
                        jsonMapper.writeValueAsString(secret),
                        infisicalProperties.projectId,
                        infisicalProperties.environmentSlug,
                        infisicalProperties.secretPath,
                    )
                }
            )
            .mapError { ex -> SecretStoreException.Operation("Failed to write secret with key $key to vault: ${ex.message}", ex) }
            .map { secret }
    }
}
