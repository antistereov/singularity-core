package io.stereov.singularity.content.article.dto

import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleColors
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.common.model.ContentAccessDetails
import io.stereov.singularity.core.global.serializer.InstantSerializer
import io.stereov.singularity.core.global.service.file.model.FileMetaData
import io.stereov.singularity.core.user.dto.UserOverviewDto
import io.stereov.singularity.core.user.model.UserDocument
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class FullArticleResponse(
    val id: String? = null,
    val key: String,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val publishedAt: Instant?,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
    val owner: UserOverviewDto,
    val path: String,
    var state: ArticleState = ArticleState.DRAFT,
    val title: String,
    val colors: ArticleColors,
    val summary: String,
    val image: FileMetaData?,
    val content: String,
    val trusted: Boolean,
    val access: ContentAccessDetails
) {

    constructor(article: Article, creator: UserDocument): this(
        article.id, article.key, article.createdAt, article.publishedAt, article.updatedAt, creator.toOverviewDto(),
        article.path, article.state, article.title, article.colors, article.summary, article.image, article.content,
        article.trusted, article.access
    )

    fun toOverview() = ArticleOverviewDto(
        id = id,
        key = key,
        createdAt = createdAt,
        publishedAt = publishedAt,
        updatedAt = updatedAt,
        path = path,
        state = state,
        title = title,
        colors = colors,
        summary = summary,
        image = image,
        access = access
    )
}
