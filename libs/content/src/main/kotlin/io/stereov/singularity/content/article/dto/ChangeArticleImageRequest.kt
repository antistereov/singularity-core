package io.stereov.singularity.content.article.dto

import io.stereov.singularity.core.global.service.file.model.FileMetaData

data class ChangeArticleImageRequest(
    val image: FileMetaData
)
