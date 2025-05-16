package io.stereov.singularity.content.article.dto

import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleColors
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.common.dto.ContentAccessDetailsResponse
import io.stereov.singularity.core.global.service.file.model.FileMetaData
import io.stereov.singularity.core.user.dto.UserOverviewDto
import io.stereov.singularity.core.user.model.UserDocument
import org.bson.types.ObjectId
import java.time.Instant

data class FullArticleResponse(
    val id: ObjectId,
    val key: String,
    val createdAt: Instant,
    val publishedAt: Instant?,
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
    val access: ContentAccessDetailsResponse,
    val tags: Set<String>
) {

    constructor(article: Article, creator: UserDocument, viewer: UserDocument?): this(
        article.id, article.key, article.createdAt, article.publishedAt, article.updatedAt, creator.toOverviewDto(),
        article.path, article.state, article.title, article.colors, article.summary, article.image, article.content,
        article.trusted, ContentAccessDetailsResponse.create(article.access, viewer), article.tags
    )

}
