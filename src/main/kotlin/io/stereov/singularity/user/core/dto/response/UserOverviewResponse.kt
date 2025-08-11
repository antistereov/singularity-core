package io.stereov.singularity.user.core.dto.response

import io.stereov.singularity.file.core.dto.FileMetadataResponse
import org.bson.types.ObjectId

data class UserOverviewResponse(
    val id: ObjectId,
    val name: String,
    val email: String,
    val avatar: FileMetadataResponse?
)
