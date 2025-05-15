package io.stereov.singularity.content.article.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateArticleRequest(
    val title: String,
)
