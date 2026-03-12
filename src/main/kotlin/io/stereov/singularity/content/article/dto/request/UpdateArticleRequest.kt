package io.stereov.singularity.content.article.dto.request

import io.stereov.singularity.content.article.model.ArticleColors
import io.stereov.singularity.database.core.model.DocumentKey
import java.util.*

data class UpdateArticleRequest(
    val title: String?,
    val summary: String?,
    val content: String?,
    val colors: ArticleColors?,
    val tags: MutableSet<DocumentKey>?,
    val locale: Locale
)