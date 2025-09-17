package io.stereov.singularity.content.article.dto.request

import java.util.*

data class CreateArticleRequest(
    val locale: Locale,
    val title: String,
    val summary: String = "",
    val content: String = "",
)
