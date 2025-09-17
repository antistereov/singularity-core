package io.stereov.singularity.content.article.dto.response

data class ArticleResponse(
    val articles: List<ArticleOverviewResponse>,
    val remainingCount: Long,
)
