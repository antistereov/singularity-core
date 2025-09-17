package io.stereov.singularity.content.article.mapper

import io.stereov.singularity.content.article.dto.request.CreateArticleRequest
import io.stereov.singularity.content.article.dto.response.ArticleOverviewResponse
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleColors
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.article.model.ArticleTranslation
import io.stereov.singularity.content.core.model.ContentAccessDetails
import io.stereov.singularity.global.properties.AppProperties
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class ArticleMapper(
    private val appProperties: AppProperties
) {

    fun createArticleOverview(article: FullArticleResponse): ArticleOverviewResponse {
        return ArticleOverviewResponse(
            id = article.id,
            key = article.key,
            createdAt = article.createdAt,
            publishedAt = article.publishedAt,
            updatedAt = article.updatedAt,
            path = article.path,
            state = article.state,
            colors = article.colors,
            image = article.image,
            locale = article.locale,
            title = article.title,
            summary = article.summary,
            access = article.access,
            tags = article.tags
        )
    }

    val basePath: String
        get() = "/articles"

    fun createArticle(req: CreateArticleRequest, key: String, ownerId: ObjectId): Article {
        val translations = mutableMapOf(req.locale to ArticleTranslation(req.title, req.summary, req.content))

        return Article(
            _id = null,
            key = key,
            createdAt = Instant.now(),
            publishedAt = null,
            updatedAt = Instant.now(),
            path = "$basePath/$key",
            state = ArticleState.DRAFT,
            colors = ArticleColors(),
            imageKey = null,
            trusted = false,
            access = ContentAccessDetails(ownerId),
            translations = translations,
            primaryLocale = req.locale
        )
    }

    fun createArticle(dto: FullArticleResponse, locale: Locale?): Article {
        val actualLocale = locale ?: appProperties.locale

        val translations = mutableMapOf(actualLocale to ArticleTranslation(dto.title, dto.summary, dto.content))

        return Article(
            _id = dto.id,
            key = dto.key,
            createdAt = dto.createdAt,
            publishedAt = dto.publishedAt,
            updatedAt = dto.updatedAt,
            path = dto.path,
            state = dto.state,
            colors = dto.colors,
            imageKey = dto.image?.key,
            trusted = dto.trusted,
            access = ContentAccessDetails.create(dto.access, dto.owner.id),
            translations = translations,
            primaryLocale = actualLocale
        )
    }
}
