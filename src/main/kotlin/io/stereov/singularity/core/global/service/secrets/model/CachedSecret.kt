package io.stereov.singularity.core.global.service.secrets.model

import java.time.Instant

data class CachedSecret(
    val secret: Secret,
    val expirationTime: Instant
)
