package io.stereov.web.admin.dto

import io.stereov.web.global.serializer.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class RotationStatusResponse(
    val isOngoing: Boolean,
    @Serializable(with = InstantSerializer::class)
    val lastRotation: Instant?,
)
