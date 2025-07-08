package io.stereov.singularity.content.common.content.dto

import io.stereov.singularity.content.common.content.model.ContentAccessRole

data class InviteUserToContentRequest(
    val email: String,
    val role: ContentAccessRole
)
