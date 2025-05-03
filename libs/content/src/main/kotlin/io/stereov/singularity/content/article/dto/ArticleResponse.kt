package io.stereov.singularity.content.article.dto

data class ArticleResponse(
    val articles: List<ArticleOverviewDto>,
    val remainingCount: Long,
)
