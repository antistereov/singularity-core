package io.stereov.singularity.secrets.local.repository

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.runSuspendCatching
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.secrets.core.exception.SecretStoreException
import io.stereov.singularity.secrets.local.data.LocalSecretEntity
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.core.select
import org.springframework.data.relational.core.query.Criteria
import org.springframework.data.relational.core.query.Query
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "singularity.secrets", value = ["store"], havingValue = "local", matchIfMissing = true)
class LocalSecretRepository(
    private val secretsTemplate: R2dbcEntityTemplate
) {

    private val logger = KotlinLogging.logger {}

    @PostConstruct
    fun init() {
        logger.info { "Initializing local secret repository" }

        runSuspendCatching {
            secretsTemplate.databaseClient.sql(
                """
                CREATE TABLE IF NOT EXISTS secrets (
                    secret_key VARCHAR(255) PRIMARY KEY,
                    secret_value VARCHAR(255) NOT NULL,
                    secret_id VARCHAR(255) NOT NULL,
                    secret_created_at TIMESTAMP NOT NULL
                )
            """.trimIndent()
            ).then().block()
        }.onFailure { ex -> logger.warn(ex) { "Failed to initialize local secret repository: ${ex.message}"} }
    }

    suspend fun findByKey(key: String): Result<LocalSecretEntity, SecretStoreException> {
        return runSuspendCatching {
            secretsTemplate
                .select<LocalSecretEntity>()
                .matching(Query.query(Criteria.where("key").`is`(key)))
                .one()
                .awaitSingleOrNull()
        }
            .mapError { ex -> SecretStoreException.Operation("Failed to fetch secret with key $key from local secret store: ${ex.message}", ex) }
            .flatMap { entity ->
                if (entity != null) {
                    Ok(entity)
                } else {
                    Err(SecretStoreException.NotFound("No secret with key $key found"))
                }
            }
    }

    suspend fun put(secret: LocalSecretEntity): Result<LocalSecretEntity, SecretStoreException> {
        return runSuspendCatching {
            secretsTemplate.databaseClient.sql(
                """
                MERGE INTO secrets (secret_key, secret_value, secret_id, secret_created_at)
                VALUES (:key, :value, :id, :created_at)
                """.trimIndent()
            )
                .bind("key", secret.key)
                .bind("value", secret.value)
                .bind("id", secret.id)
                .bind("created_at", secret.createdAt)
                .then()
                .awaitFirstOrNull()
        }
            .map { secret }
            .mapError { ex -> SecretStoreException.Operation("Failed to save secret ${secret.key} in local secret store: ${ex.message}", ex) }
    }
}
