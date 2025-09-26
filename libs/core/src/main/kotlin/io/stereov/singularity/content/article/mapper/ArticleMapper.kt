package io.stereov.singularity.content.article.mapper

import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.article.dto.request.CreateArticleRequest
import io.stereov.singularity.content.article.dto.response.ArticleOverviewResponse
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleColors
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.article.model.ArticleTranslation
import io.stereov.singularity.content.core.dto.response.ContentAccessDetailsResponse
import io.stereov.singularity.content.core.model.ContentAccessDetails
import io.stereov.singularity.content.tag.mapper.TagMapper
import io.stereov.singularity.content.tag.service.TagService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import org.bson.types.ObjectId
import org.springframework.stereotype.Component
import java.time.Instant
import java.util.*

@Component
class ArticleMapper(
    private val appProperties: AppProperties,
    private val authorizationService: AuthorizationService,
    private val userService: UserService,
    private val translateService: TranslateService,
    private val tagMapper: TagMapper,
    private val tagService: TagService,
    private val fileStorage: FileStorage,
    private val userMapper: UserMapper
) {

    fun createOverview(article: FullArticleResponse): ArticleOverviewResponse {
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
        )
    }

    suspend fun createFullResponse(article: Article, locale: Locale?, owner: UserDocument? = null): FullArticleResponse {
        val currentUser = authorizationService.getAuthenticationOrNull()

        val actualOwner = owner ?: userService.findById(article.access.ownerId)
        val access = ContentAccessDetailsResponse.create(article.access, currentUser)
        val (articleLang, translation) = translateService.translate(article, locale)

        val tags = article.tags.map { key -> tagMapper.createTagResponse(tagService.findByKey(key), articleLang) }

        val image = article.imageKey?.let { fileStorage.metadataResponseByKeyOrNull(it) }

        return FullArticleResponse(
            id = article.id,
            key = article.key,
            createdAt = article.createdAt,
            publishedAt = article.publishedAt,
            updatedAt = article.updatedAt,
            owner = userMapper.toOverview(actualOwner),
            path = article.path,
            state = article.state,
            colors = article.colors,
            image = image,
            trusted = article.trusted,
            access = access,
            locale = articleLang,
            title = translation.title,
            summary = translation.summary,
            content = translation.content,
            tags = tags
        )
    }

    suspend fun createOverview(article: Article, locale: Locale?): ArticleOverviewResponse {
        return createOverview(createFullResponse(article, locale))
    }
}
