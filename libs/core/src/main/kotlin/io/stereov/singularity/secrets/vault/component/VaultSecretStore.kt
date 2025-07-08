package io.stereov.singularity.secrets.vault.component

import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.core.exception.model.SecretStoreException
import io.stereov.singularity.secrets.core.model.Secret
import io.stereov.singularity.secrets.vault.properties.VaultSecretStoreProperties
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.vault.core.ReactiveVaultOperations
import java.time.Instant
import java.util.*

@Component
@ConditionalOnProperty(prefix = "singularity.secrets", value = ["store"], havingValue = "hashicorp", matchIfMissing = false)
class VaultSecretStore(
    private val properties: VaultSecretStoreProperties,
    private val appProperties: AppProperties,
    private val vaultOperations: ReactiveVaultOperations
) : SecretStore {

    private val apiPath: String
        get() = "/v1/${properties.engine}/data/${appProperties.slug}"

    private fun getSecretPath(key: String): String = "$apiPath/$key"

    override suspend fun getOrNull(key: String): Secret? {
        val secretPath = getSecretPath(key)

        val data = vaultOperations.read(secretPath).awaitSingle().data
            ?: return null

        return data.toSecret(key)
    }


    override suspend fun put(
        key: String,
        value: String,
        note: String
    ): Secret {
        val secret = Secret(UUID.randomUUID(), key, value, Instant.now())
        vaultOperations.write(getSecretPath(key), secret.toMap()).awaitSingle()

        return secret
    }

    val idField = "id"
    val valueField = "value"
    val createdAtField = "createdAt"
    val keyField = "key"


    private fun Map<String, Any?>.toSecret(key: String): Secret {
        val id = (this[idField] as? UUID)
            ?: throw SecretStoreException("Cannot read secret with key \"$key\": $idField is not of type UUID")
        val value = this[valueField] as? String
            ?: throw SecretStoreException("Cannot read secret with key \"$key\": $valueField is not of type String")
        val createdAt = this[createdAtField] as? Instant
            ?: throw SecretStoreException("Cannot read secret with key \"$key\": $createdAtField is not of type Instant")

        return Secret(id, key, value, createdAt)
    }

    private fun Secret.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()

        map[idField] = this.id
        map[valueField] = this.value
        map[createdAtField] = this.createdAt
        map[keyField] = this.key

        return map
    }
}