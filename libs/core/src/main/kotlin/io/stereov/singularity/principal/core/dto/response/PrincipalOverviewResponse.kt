package io.stereov.singularity.principal.core.dto.response

import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.principal.core.model.Role
import org.bson.types.ObjectId

data class PrincipalOverviewResponse(
    val id: ObjectId,
    val name: String,
    val avatar: FileMetadataResponse?,
    val roles: Set<Role>
)
