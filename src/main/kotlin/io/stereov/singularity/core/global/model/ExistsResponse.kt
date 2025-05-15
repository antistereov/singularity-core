package io.stereov.singularity.core.global.model

import kotlinx.serialization.Serializable

@Serializable
data class ExistsResponse(
    val exists: Boolean
)
