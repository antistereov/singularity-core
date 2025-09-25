package io.stereov.singularity.content.article.helper

import io.stereov.singularity.content.article.dto.response.ArticleOverviewResponse

data class ArticlesResponse(
    val articles: List<ArticleOverviewResponse>,
    val remainingCount: Long,
)
