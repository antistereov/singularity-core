package io.stereov.singularity.core.user.dto

import io.stereov.singularity.core.global.service.file.model.FileMetaData
import kotlinx.serialization.Serializable

@Serializable
data class UserOverviewDto(
    val id: String,
    val name: String,
    val avatar: FileMetaData?
)
