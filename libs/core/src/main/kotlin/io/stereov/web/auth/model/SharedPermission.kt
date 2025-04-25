package io.stereov.web.auth.model

import kotlinx.serialization.Serializable

@Serializable
data class SharedPermission(
    val userId: String,
    val permissions: Set<Permission>
)
