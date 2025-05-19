package io.stereov.singularity.content.article.dto

import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleColors
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.common.content.dto.ContentAccessDetailsResponse
import io.stereov.singularity.core.global.language.model.Language
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
    val colors: ArticleColors,
    val image: FileMetaData?,
    val lang: Language,
    val title: String,
    val summary: String,
    val content: String,
    val trusted: Boolean,
    val access: ContentAccessDetailsResponse,
    val tags: Set<String>
) {

    companion object {

        fun create(article: Article, owner: UserDocument, viewer: UserDocument?, lang: Language): FullArticleResponse {
            val access = ContentAccessDetailsResponse.create(article.access, viewer)
            val (lang, translation) = article.translate(lang)

            return FullArticleResponse(
                id = article.id,
                key = article.key,
                createdAt = article.createdAt,
                publishedAt = article.publishedAt,
                updatedAt = article.updatedAt,
                owner = owner.toOverviewDto(),
                path = article.path,
                state = article.state,
                colors = article.colors,
                image = article.image,
                trusted = article.trusted,
                access = access,
                lang = lang,
                title = translation.title,
                summary = translation.summary,
                content = translation.content,
                tags = article.tags
            )
        }
    }
}
