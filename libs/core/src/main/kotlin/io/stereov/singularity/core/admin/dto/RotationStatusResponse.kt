package io.stereov.singularity.core.admin.dto

import io.stereov.singularity.core.global.serializer.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class RotationStatusResponse(
    val isOngoing: Boolean,
    @Serializable(with = InstantSerializer::class)
    val lastRotation: Instant?,
)
