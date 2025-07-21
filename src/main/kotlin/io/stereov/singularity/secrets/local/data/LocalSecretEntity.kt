package io.stereov.singularity.secrets.local.data

import io.stereov.singularity.secrets.core.model.Secret
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.*

@Table("secrets")
data class LocalSecretEntity(
    @Id
    @Column("secret_key")
    val key: String,

    @Column("secret_id")
    val id: String,

    @Column("secret_value")
    val value: String,

    @Column("secret_created_at")
    val createdAt: Instant = Instant.now()
) {

    fun toSecret(): Secret {
        return Secret(
            UUID.fromString(id), key, value, createdAt
        )
    }
}
