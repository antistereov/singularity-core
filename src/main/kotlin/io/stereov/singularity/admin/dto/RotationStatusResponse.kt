package io.stereov.singularity.admin.dto

import io.stereov.singularity.global.serializer.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class RotationStatusResponse(
    val isOngoing: Boolean,
    @Serializable(with = InstantSerializer::class)
    val lastRotation: Instant?,
)
