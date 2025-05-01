package io.stereov.singularity.content.article.model

import io.stereov.singularity.core.global.service.file.model.FileMetaData
import kotlinx.serialization.Serializable

@Serializable
data class ArticleContent(
    val content: String,
    val files: List<FileMetaData>,
)
