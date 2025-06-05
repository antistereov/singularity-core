package io.stereov.singularity.core.secrets.model

import java.time.Instant
import java.util.*

data class Secret(
    val id: UUID,
    val key: String,
    val value: String,
    val createdAt: Instant
)
