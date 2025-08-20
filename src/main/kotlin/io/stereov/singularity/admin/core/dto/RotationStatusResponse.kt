package io.stereov.singularity.admin.core.dto

import java.time.Instant

data class RotationStatusResponse(
    val isOngoing: Boolean,
    val lastRotation: Instant?,
)
