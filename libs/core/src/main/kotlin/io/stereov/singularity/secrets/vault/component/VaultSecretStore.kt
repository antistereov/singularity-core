package io.stereov.singularity.secrets.vault.component

import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.secrets.core.component.SecretCache
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.core.exception.model.SecretStoreException
import io.stereov.singularity.secrets.core.model.Secret
import io.stereov.singularity.secrets.vault.properties.VaultSecretStoreProperties
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.vault.core.ReactiveVaultOperations
import java.time.Instant
import java.util.*

@Component
@ConditionalOnProperty(prefix = "singularity.secrets", value = ["store"], havingValue = "vault", matchIfMissing = false)
class VaultSecretStore(
    private val properties: VaultSecretStoreProperties,
    private val appProperties: AppProperties,
    private val vaultOperations: ReactiveVaultOperations,
    override val secretCache: SecretCache
) : SecretStore {

    private val apiPath: String
        get() = "${properties.engine}/data/${appProperties.slug}"

    private fun getSecretPath(key: String): String = "$apiPath/$key"

    override suspend fun doGetOrNull(key: String): Secret? {
        val secretPath = getSecretPath(key)

        val data = vaultOperations.read(secretPath).awaitSingleOrNull()?.data
            ?: return null

        return data.toSecret(key)
    }


    override suspend fun doPut(
        key: String,
        value: String,
        note: String
    ): Secret {
        val secret = Secret(UUID.randomUUID(), key, value, Instant.now())
        vaultOperations.write(getSecretPath(key), mapOf("data" to secret.toMap())).awaitSingle()

        return secret
    }

    val dataField = "data"
    val idField = "id"
    val valueField = "value"
    val createdAtField = "createdAt"
    val keyField = "key"


    private fun Map<String, Any?>.toSecret(key: String): Secret {
        val data = this[dataField] as? Map<*, *>
            ?: throw SecretStoreException("Cannot read secret with key \"$key\": $dataField does not exist or is corrupt")

        val idString = data[idField] as? String
            ?: throw SecretStoreException("Cannot read secret with key \"$key\": $idField is not of type String")
        val value = data[valueField] as? String
            ?: throw SecretStoreException("Cannot read secret with key \"$key\": $valueField is not of type String")
        val createdAtString = data[createdAtField] as? String
            ?: throw SecretStoreException("Cannot read secret with key \"$key\": $createdAtField is not of type String")

        val id = try { UUID.fromString(idString) } catch (e: Exception) {
            throw SecretStoreException("Cannot read secret with key \"$key\": $idField is not of type UUID", e)
        }

        val createdAt = try {
            Instant.parse(createdAtString)
        } catch (e: Exception) {
            throw SecretStoreException("Cannot read secret with key \"$key\": $createdAtField is not of type Instant", e)
        }

        return Secret(id, key, value, createdAt)
    }

    private fun Secret.toMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()

        map[idField] = this.id.toString()
        map[valueField] = this.value
        map[createdAtField] = this.createdAt.toString()
        map[keyField] = this.key

        return map
    }
}
