package io.stereov.singularity.content.article.model

import kotlinx.serialization.Serializable

@Serializable
data class ArticleColors(
    val text: String,
    val background: String,
)
