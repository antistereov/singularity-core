package io.stereov.singularity.secrets.local.data

import io.stereov.singularity.secrets.core.model.Secret
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.Instant
import java.util.*

@Table("secrets")
data class LocalSecretEntity(
    @Id val key: String,
    val id: String,
    val value: String,
    @Column("created_at") val createdAt: Instant = Instant.now()
) {

    fun toSecret(): Secret {
        return Secret(
            UUID.fromString(id), key, value, createdAt
        )
    }
}
