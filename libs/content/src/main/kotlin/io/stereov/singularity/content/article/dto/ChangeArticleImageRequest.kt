package io.stereov.singularity.content.article.dto

import io.stereov.singularity.file.model.FileMetaData


data class ChangeArticleImageRequest(
    val image: FileMetaData
)
