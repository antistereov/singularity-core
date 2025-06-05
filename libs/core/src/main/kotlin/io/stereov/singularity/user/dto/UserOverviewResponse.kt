package io.stereov.singularity.user.dto

import io.stereov.singularity.global.service.file.model.FileMetaData
import org.bson.types.ObjectId

data class UserOverviewResponse(
    val id: ObjectId,
    val name: String,
    val email: String,
    val avatar: FileMetaData?
)
