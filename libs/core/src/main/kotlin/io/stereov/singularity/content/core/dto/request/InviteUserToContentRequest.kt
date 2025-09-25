package io.stereov.singularity.content.core.dto.request

import io.stereov.singularity.content.core.model.ContentAccessRole

data class InviteUserToContentRequest(
    val email: String,
    val role: ContentAccessRole
)
