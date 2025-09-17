package io.stereov.singularity.content.article.dto.request

import io.stereov.singularity.content.article.model.ArticleState

data class ChangeArticleStateRequest(
    val state: ArticleState
)
