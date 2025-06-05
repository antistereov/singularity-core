package io.stereov.singularity.admin.dto

import java.time.Instant

data class RotationStatusResponse(
    val isOngoing: Boolean,
    val lastRotation: Instant?,
)
