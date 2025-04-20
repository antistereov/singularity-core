package io.stereov.web.admin.dto

import kotlinx.serialization.Serializable

@Serializable
data class RotationStatusResponse(
    val isOngoing: Boolean,
    val lastRotation: String,
)
