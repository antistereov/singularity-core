package io.stereov.singularity.content.article.model

import kotlinx.serialization.Serializable

@Serializable
data class ArticleColors(
    val text: String = "white",
    val background: String = "#00008B",
)
