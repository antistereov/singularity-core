package io.stereov.singularity.content.core.dto

import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.user.core.dto.response.UserOverviewResponse

data class UserContentAccessDetails(
    val user: UserOverviewResponse,
    val role: ContentAccessRole
)
