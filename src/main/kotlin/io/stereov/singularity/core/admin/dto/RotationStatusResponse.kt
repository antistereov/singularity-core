package io.stereov.singularity.core.admin.dto

import java.time.Instant

data class RotationStatusResponse(
    val isOngoing: Boolean,
    val lastRotation: Instant?,
)
