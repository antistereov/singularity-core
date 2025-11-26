package io.stereov.singularity.content.core.dto.response

import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.principal.core.dto.response.UserOverviewResponse

data class UserContentAccessDetails(
    val user: UserOverviewResponse,
    val role: ContentAccessRole
)
