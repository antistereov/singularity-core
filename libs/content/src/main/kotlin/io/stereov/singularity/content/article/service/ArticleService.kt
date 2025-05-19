package io.stereov.singularity.content.article.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.content.article.dto.CreateArticleRequest
import io.stereov.singularity.content.article.dto.FullArticleResponse
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.repository.ArticleRepository
import io.stereov.singularity.content.common.content.service.ContentService
import io.stereov.singularity.content.common.content.util.toSlug
import io.stereov.singularity.core.global.language.model.Language
import io.stereov.singularity.content.common.util.AccessCriteria
import io.stereov.singularity.core.auth.service.AuthenticationService
import io.stereov.singularity.core.global.util.paginateWithQuery
import io.stereov.singularity.core.user.model.Role
import io.stereov.singularity.core.user.model.UserDocument
import io.stereov.singularity.core.user.service.UserService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.util.*

@Service
class ArticleService(
    repository: ArticleRepository,
    private val userService: UserService,
    private val authenticationService: AuthenticationService,
    private val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val accessCriteria: AccessCriteria,
) : ContentService<Article>(repository, Article::class.java) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun create(req: CreateArticleRequest, lang: Language): FullArticleResponse {
        logger.debug { "Creating article with title ${req.title}" }

        val user = authenticationService.getCurrentUser()

        val baseKey = req.title.toSlug()
        val key = if (existsByKey(baseKey)) {
            "$baseKey-${UUID.randomUUID().toString().substring(0, 8)}"
        } else baseKey

        val article = Article.create(req, key,user.id)
        val savedArticle = save(article)

        return fullArticledResponseFrom(savedArticle, lang,user)
    }

    suspend fun setTrustedState(key: String, trusted: Boolean): Article {
        authenticationService.validateAuthorization(Role.ADMIN)

        val article = findByKey(key)

        article.trusted = trusted
        return save(article)
    }

    suspend fun getFullArticleResponseByKey(key: String, lang: Language): FullArticleResponse {
        return fullArticledResponseFrom(findByKey(key), lang)
    }

    suspend fun fullArticledResponseFrom(article: Article, lang: Language, owner: UserDocument? = null): FullArticleResponse {
        val currentUser = authenticationService.getCurrentUserOrNull()

        val actualOwner = owner ?: userService.findById(article.access.ownerId)
        return FullArticleResponse.create(article, actualOwner, currentUser, lang)
    }

    suspend fun getArticles(pageable: Pageable, tags: List<String>, lang: Language): Page<Article> {
        val criteria = if (tags.isEmpty()) {
            accessCriteria.getViewCriteria()
        } else {
            Criteria().andOperator(
                accessCriteria.getViewCriteria(),
                Criteria.where(Article::tags.name).`in`(tags)
            )
        }

        val query = Query.query(criteria)

        return paginateWithQuery(reactiveMongoTemplate, query, pageable, contentClass)
    }
}
