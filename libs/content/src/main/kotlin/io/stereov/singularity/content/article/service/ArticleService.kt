package io.stereov.singularity.content.article.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.content.article.dto.CreateArticleRequest
import io.stereov.singularity.content.article.dto.FullArticleResponse
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.repository.ArticleRepository
import io.stereov.singularity.content.common.service.ContentService
import io.stereov.singularity.content.common.util.toSlug
import io.stereov.singularity.core.auth.service.AuthenticationService
import io.stereov.singularity.core.user.model.Role
import io.stereov.singularity.core.user.model.UserDocument
import io.stereov.singularity.core.user.service.UserService
import org.springframework.stereotype.Service
import java.util.*

@Service
class ArticleService(
    repository: ArticleRepository,
    private val userService: UserService,
    private val authenticationService: AuthenticationService,
) : ContentService<Article>(repository, Article::class.java) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun create(req: CreateArticleRequest): FullArticleResponse {
        logger.debug { "Creating article with title ${req.title}" }
        val user = authenticationService.getCurrentUser()

        val baseKey = req.title.toSlug()
        val key = if (existsByKey(baseKey)) {
            "$baseKey-${UUID.randomUUID().toString().substring(0, 8)}"
        } else baseKey

        val article = Article.create(key = key, ownerId = user.id)
        val savedArticle = save(article)

        return fullArticledDtoFrom(savedArticle, user)
    }

    suspend fun findFullArticleDtoByKey(key: String): FullArticleResponse {
        return fullArticledDtoFrom(findByKey(key))
    }

    suspend fun setTrustedState(key: String, trusted: Boolean): Article {
        authenticationService.validateAuthorization(Role.ADMIN)

        val article = findByKey(key)

        article.trusted = trusted
        return save(article)
    }

    suspend fun fullArticledDtoFrom(article: Article, owner: UserDocument? = null): FullArticleResponse {
        val actualOwner = owner ?: userService.findById(article.access.ownerId)
        return FullArticleResponse(article, actualOwner)
    }
}
