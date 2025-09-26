package io.stereov.singularity.content.article.dto.request

import io.stereov.singularity.content.article.model.ArticleColors
import java.util.*

data class UpdateArticleRequest(
    val tags: List<String>? = emptyList(),
    val title: String?,
    val colors: ArticleColors?,
    val summary: String?,
    val content: String?,
    val locale: Locale
)