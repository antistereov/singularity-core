package io.stereov.singularity.content.article.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.content.article.dto.ArticleOverviewResponse
import io.stereov.singularity.content.article.dto.ArticleResponse
import io.stereov.singularity.content.article.dto.FullArticleResponse
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.article.repository.ArticleRepository
import io.stereov.singularity.content.common.content.dto.ContentAccessDetailsResponse
import io.stereov.singularity.content.common.content.service.ContentService
import io.stereov.singularity.content.common.tag.service.TagService
import io.stereov.singularity.content.common.util.AccessCriteria
import io.stereov.singularity.core.auth.service.AuthenticationService
import io.stereov.singularity.core.global.language.model.Language
import io.stereov.singularity.core.user.model.UserDocument
import io.stereov.singularity.core.user.service.UserService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service

@Service
class ArticleService(
    repository: ArticleRepository,
    private val userService: UserService,
    private val authenticationService: AuthenticationService,
    private val tagService: TagService,
    private val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val accessCriteria: AccessCriteria,
) : ContentService<Article>(repository, Article::class.java) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    private val isPublished = Criteria.where(Article::state.name).`is`(ArticleState.PUBLISHED.toString())

    suspend fun getFullArticleResponseByKey(key: String, lang: Language): FullArticleResponse {
        return fullArticledResponseFrom(findByKey(key), lang)
    }

    suspend fun articleOverviewFrom(article: Article, lang: Language): ArticleOverviewResponse {
        return fullArticledResponseFrom(article, lang).toOverview()
    }

    suspend fun fullArticledResponseFrom(article: Article, lang: Language, owner: UserDocument? = null): FullArticleResponse {
        val currentUser = authenticationService.getCurrentUserOrNull()

        val actualOwner = owner ?: userService.findById(article.access.ownerId)
        val access = ContentAccessDetailsResponse.create(article.access, currentUser)
        val (lang, translation) = article.translate(lang)

        val tags = article.tags.map { key -> tagService.findByKey(key).toResponse(lang) }

        return FullArticleResponse(
            id = article.id,
            key = article.key,
            createdAt = article.createdAt,
            publishedAt = article.publishedAt,
            updatedAt = article.updatedAt,
            owner = actualOwner.toOverviewDto(),
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
            tags = tags
        )
    }

    suspend fun getArticles(pageable: Pageable, tags: List<String>, lang: Language): Page<ArticleOverviewResponse> {
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

        val overviews = content.map { articleOverviewFrom(it, lang) }

        return PageImpl(overviews, pageable, totalCount)
    }

    suspend fun getAccessibleArticles(
        limit: Long = 10,
        afterId: String? = null,
        lang: Language
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

        return ArticleResponse(articles.map { articleOverviewFrom(it, lang) }, remainingCount)
    }
}
