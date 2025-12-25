package io.stereov.singularity.content.core.dto.response

import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.principal.core.dto.response.PrincipalOverviewResponse

data class UserContentAccessDetails(
    val user: PrincipalOverviewResponse,
    val role: ContentAccessRole
)
