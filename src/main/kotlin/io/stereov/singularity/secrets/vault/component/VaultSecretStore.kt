package io.stereov.singularity.secrets.vault.component

import com.github.michaelbull.result.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.secrets.core.component.SecretCache
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.core.exception.SecretStoreException
import io.stereov.singularity.secrets.core.model.Secret
import io.stereov.singularity.secrets.vault.properties.VaultSecretStoreProperties
import kotlinx.coroutines.reactive.awaitSingle
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
    override val logger = KotlinLogging.logger {}

    private fun getSecretPath(key: String): String = "$apiPath/$key"

    override suspend fun doGet(key: String): Result<Secret, SecretStoreException> {
        val secretPath = getSecretPath(key)

        return runCatching {
            vaultOperations.read(secretPath).awaitSingle()?.data
        }
            .mapError { ex -> SecretStoreException.Operation("Failed to generate secret with key $key from vault: ${ex.message}", ex) }
            .flatMap { data ->
                if (data != null) {
                    Ok(data)
                } else {
                    Err(SecretStoreException.NotFound("No secret with key $key found in vault"))
                }
            }
            .flatMap { it.toSecret(key) }
    }


    override suspend fun doPut(
        key: String,
        value: String,
        note: String
    ): Result<Secret, SecretStoreException> {
        val secret = Secret(UUID.randomUUID(), key, value, Instant.now())

        return runCatching {
            vaultOperations.write(getSecretPath(key), mapOf("data" to secret.toMap())).awaitSingle()
        }
            .mapError { ex -> SecretStoreException.Operation("Failed to write secret with key $key to vault: ${ex.message}", ex) }
            .map { secret }
    }

    val dataField = "data"
    val idField = "id"
    val valueField = "value"
    val createdAtField = "createdAt"
    val keyField = "key"


    private fun Map<String, Any?>.toSecret(key: String): Result<Secret, SecretStoreException> = binding {
        val data = (this@toSecret[dataField] as? Map<*, *>)
            .toResultOr { SecretStoreException.Operation("Cannot read secret with key \"$key\": $dataField does not exist or is corrupt") }
            .bind()

        val idString = (data[idField] as? String)
            .toResultOr { SecretStoreException.Operation("Cannot read secret with key \"$key\": $idField is not of type String") }
            .bind()

        val value = (data[valueField] as? String)
            .toResultOr { SecretStoreException.Operation("Cannot read secret with key \"$key\": $valueField is not of type String") }
            .bind()

        val createdAtString = (data[createdAtField] as? String)
            .toResultOr { SecretStoreException.Operation("Cannot read secret with key \"$key\": $createdAtField is not of type String") }
            .bind()

        val id = runCatching { UUID.fromString(idString) }
            .mapError { ex -> SecretStoreException.Operation("Cannot read secret with key \"$key\": $idField is not of type UUID", ex) }
            .bind()

        val createdAt = runCatching { Instant.parse(createdAtString) }
            .mapError { ex -> SecretStoreException.Operation("Cannot read secret with key \"$key\": $createdAtField is not of type Instant", ex) }
            .bind()

        Secret(id, key, value, createdAt)
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
