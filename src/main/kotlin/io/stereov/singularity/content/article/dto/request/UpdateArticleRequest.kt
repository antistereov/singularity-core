package io.stereov.singularity.content.article.dto.request

import io.stereov.singularity.content.article.model.ArticleColors
import java.util.*

data class UpdateArticleRequest(
    val title: String?,
    val summary: String?,
    val content: String?,
    val colors: ArticleColors?,
    val tags: MutableSet<String>?,
    val locale: Locale
)