package io.stereov.singularity.user.core.dto.response

import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.user.core.model.Role
import org.bson.types.ObjectId

data class UserOverviewResponse(
    val id: ObjectId,
    val name: String,
    val avatar: FileMetadataResponse?,
    val roles: Set<Role>
)
