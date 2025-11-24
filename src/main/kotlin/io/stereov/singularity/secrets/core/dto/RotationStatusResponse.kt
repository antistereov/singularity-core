package io.stereov.singularity.secrets.core.dto

import io.stereov.singularity.secrets.core.model.RotationInformation

data class RotationStatusResponse(
    val isOngoing: Boolean,
    val state: Map<String, RotationInformation>,
)