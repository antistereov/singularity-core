package io.stereov.singularity.content.common.content.dto

import io.stereov.singularity.content.common.content.model.ContentAccessRole
import io.stereov.singularity.core.user.dto.UserOverviewResponse

data class UserContentAccessDetails(
    val user: UserOverviewResponse,
    val role: ContentAccessRole
)
