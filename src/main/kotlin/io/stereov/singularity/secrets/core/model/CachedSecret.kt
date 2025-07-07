package io.stereov.singularity.secrets.core.model

import java.time.Instant

data class CachedSecret(
    val secret: Secret,
    val expirationTime: Instant
)
