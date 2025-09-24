package io.stereov.singularity.content.article.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.article.dto.response.ArticleOverviewResponse
import io.stereov.singularity.content.article.dto.response.ArticleResponse
import io.stereov.singularity.content.article.dto.response.FullArticleResponse
import io.stereov.singularity.content.article.mapper.ArticleMapper
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.article.repository.ArticleRepository
import io.stereov.singularity.content.core.component.AccessCriteria
import io.stereov.singularity.content.core.dto.ContentAccessDetailsResponse
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.content.core.service.ContentService
import io.stereov.singularity.content.tag.mapper.TagMapper
import io.stereov.singularity.content.tag.service.TagService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.util.*

@Service
class ArticleService(
    override val repository: ArticleRepository,
    private val userService: UserService,
    override val authorizationService: AuthorizationService,
    private val tagService: TagService,
    override val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val accessCriteria: AccessCriteria,
    private val fileStorage: FileStorage,
    private val userMapper: UserMapper,
    private val articleMapper: ArticleMapper,
    private val tagMapper: TagMapper,
    private val translateService: TranslateService
) : ContentService<Article> {

    override val logger: KLogger = KotlinLogging.logger {}
    override val collectionClazz: Class<Article> = Article::class.java

    private val isPublished = Criteria.where(Article::state.name).`is`(ArticleState.PUBLISHED.toString())

    suspend fun getFullArticleResponseByKey(key: String, locale: Locale?): FullArticleResponse {
        return fullArticledResponseFrom(findAuthorizedByKey(key, ContentAccessRole.VIEWER), locale)
    }

    suspend fun articleOverviewFrom(article: Article, locale: Locale?): ArticleOverviewResponse {
        return articleMapper.createArticleOverview(fullArticledResponseFrom(article, locale))
    }

    suspend fun fullArticledResponseFrom(article: Article, locale: Locale?, owner: UserDocument? = null): FullArticleResponse {
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

    suspend fun getArticles(pageable: Pageable, tags: List<String>, locale: Locale?): Page<ArticleOverviewResponse> {
        val criteria = if (tags.isEmpty()) {
            accessCriteria.getViewCriteria()
        } else {
            Criteria().andOperator(
                accessCriteria.getViewCriteria(),
                Criteria.where(Article::tags.name).`in`(tags)
            )
        }

        val query = Query.query(criteria)

        val totalCount = reactiveMongoTemplate.count(query, Article::class.java)
            .awaitFirstOrNull() ?: 0

        query.with(pageable)

        val content = reactiveMongoTemplate.find(query, Article::class.java)
            .collectList()
            .awaitFirstOrNull()
            ?: emptyList()

        val overviews = content.map { articleOverviewFrom(it, locale) }

        return PageImpl(overviews, pageable, totalCount)
    }

    suspend fun getAccessibleArticles(
        limit: Long = 10,
        afterId: String? = null,
        locale: Locale?
    ): ArticleResponse {
        logger.debug { "Getting accessible articles limit=$limit${afterId?.let { " after $it" } ?: ""}" }

        val query = Query()
            .with(Sort.by(Sort.Order.desc("_id")))
            .addCriteria(Criteria().andOperator(
                accessCriteria.getViewCriteria(),
                isPublished
            ))
            .limit(limit.toInt())

        logger.debug { "Query: $query" }

        if (afterId != null) {
            query.addCriteria(Criteria.where(Article::id.name).lt(afterId))
        }

        val articles = reactiveMongoTemplate.find(query, Article::class.java)
            .collectList()
            .awaitFirstOrNull() ?: emptyList()

        val remainingCount = if (articles.isNotEmpty()) {
            reactiveMongoTemplate.count(
                Query()
                    .addCriteria(Criteria().andOperator(
                        accessCriteria.getViewCriteria(),
                        isPublished
                    ))
                    .addCriteria(Criteria.where(Article::id.name).lt(articles.last().id)),
                Article::class.java
            ).awaitFirstOrNull() ?: 0
        } else 0

        return ArticleResponse(articles.map { articleOverviewFrom(it, locale) }, remainingCount)
    }
}
