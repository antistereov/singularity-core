package io.stereov.singularity.content.article.dto

import io.stereov.singularity.content.article.model.ArticleColors

data class ChangeArticleColorsRequest(
    val colors: ArticleColors
)
