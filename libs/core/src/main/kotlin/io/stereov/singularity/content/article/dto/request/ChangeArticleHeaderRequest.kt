package io.stereov.singularity.content.article.dto.request

import io.stereov.singularity.content.article.model.ArticleColors
import java.util.*

data class ChangeArticleHeaderRequest(
    val locale: Locale,
    val title: String,
    val colors: ArticleColors,
)
