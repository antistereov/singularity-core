package io.stereov.singularity.content.article.dto

import io.stereov.singularity.core.auth.model.AccessType
import io.stereov.singularity.core.global.serializer.InstantSerializer
import io.stereov.singularity.core.global.service.file.model.FileMetaData
import io.stereov.singularity.core.user.dto.UserOverviewDto
import io.stereov.singularity.core.user.model.UserDocument
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleColors
import io.stereov.singularity.content.article.model.ArticleContent
import io.stereov.singularity.content.article.model.ArticleState
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class FullArticleDto(
    val id: String? = null,
    val key: String,
    @Serializable(with = InstantSerializer::class)
    val createdAt: Instant,
    @Serializable(with = InstantSerializer::class)
    val publishedAt: Instant?,
    @Serializable(with = InstantSerializer::class)
    val updatedAt: Instant,
    val creator: UserOverviewDto?,
    val path: String,
    val state: ArticleState = ArticleState.DRAFT,
    val title: String,
    val colors: ArticleColors,
    val summary: String,
    val image: FileMetaData,
    val content: ArticleContent,
    val accessType: AccessType,
    val canView: List<String>,
    val canEdit: List<String>
) {

    constructor(article: Article, creator: UserDocument?): this(
        article.id, article.key, article.createdAt, article.publishedAt, article.updatedAt, creator?.toOverviewDto(),
        article.path, article.state, article.title, article.colors, article.summary, article.image, article.content,
        article.accessType, article.canView, article.canEdit
    )
}
