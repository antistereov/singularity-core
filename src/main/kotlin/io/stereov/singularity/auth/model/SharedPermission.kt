package io.stereov.singularity.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class SharedPermission(
    val userId: String,
    val permissions: Set<Permission>
)
