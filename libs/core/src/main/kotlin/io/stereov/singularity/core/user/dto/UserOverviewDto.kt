package io.stereov.singularity.core.user.dto

import io.stereov.singularity.core.global.service.file.model.FileMetaData
import org.bson.types.ObjectId

data class UserOverviewDto(
    val id: ObjectId,
    val name: String,
    val avatar: FileMetaData?
)
