package io.stereov.singularity.content.article.dto

import io.stereov.singularity.content.article.model.ArticleState

data class ChangeArticleStateRequest(
    val state: ArticleState
)
