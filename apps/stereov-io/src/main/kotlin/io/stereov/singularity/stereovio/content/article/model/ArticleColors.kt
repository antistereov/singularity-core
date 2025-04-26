package io.stereov.singularity.stereovio.content.article.model

import kotlinx.serialization.Serializable

@Serializable
data class ArticleColors(
    val text: String,
    val background: String,
)
