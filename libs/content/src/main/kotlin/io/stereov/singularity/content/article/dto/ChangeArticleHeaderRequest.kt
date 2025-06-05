package io.stereov.singularity.content.article.dto

import io.stereov.singularity.content.article.model.ArticleColors
import io.stereov.singularity.global.language.model.Language

data class ChangeArticleHeaderRequest(
    val lang: Language,
    val title: String,
    val colors: ArticleColors,
)
