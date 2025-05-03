package io.stereov.singularity.content.article.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.content.article.dto.FullArticleDto
import io.stereov.singularity.content.article.exception.model.ArticleKeyExistsException
import io.stereov.singularity.content.article.exception.model.InvalidArticleRequestException
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.repository.ArticleRepository
import io.stereov.singularity.core.auth.service.AuthenticationService
import io.stereov.singularity.core.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.core.user.model.Role
import io.stereov.singularity.core.user.service.UserService
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Service

@Service
class ArticleService(
    private val repository: ArticleRepository,
    private val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val userService: UserService,
    private val authenticationService: AuthenticationService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun save(article: Article): Article {
        logger.debug { "Saving article" }

        return repository.save(article)
    }

    suspend fun save(dto: FullArticleDto): FullArticleDto {
        authenticationService.validateAuthentication()

        val article = articleFromDto(dto)
        val savedArticle = save(article)
        return fullArticledDtoFrom(savedArticle)
    }

    suspend fun findByIdOrNull(id: String): Article? {
        logger.debug { "Finding article by ID $id" }

        return repository.findById(id)
    }

    suspend fun findById(id: String): Article {
        return findByIdOrNull(id) ?: throw DocumentNotFoundException("No article with ID $id found")
    }

    suspend fun findByKeyOrNull(key: String): Article? {
        logger.debug { "Fining article by key" }

        return repository.findByKey(key)
    }

    suspend fun findByKey(key: String): Article {
        return findByKeyOrNull(key) ?: throw DocumentNotFoundException("No article with key $key found")
    }

    suspend fun findFullArticleDtoByKey(key: String): FullArticleDto {
        return fullArticledDtoFrom(findByKey(key))
    }

    suspend fun setTrustedState(key: String, trusted: Boolean): Article {
        authenticationService.validateAuthorization(Role.ADMIN)

        val article = findByKey(key)
        article.trusted = trusted
        return save(article)
    }

    suspend fun fullArticledDtoFrom(article: Article): FullArticleDto {
        val creator = userService.findById(article.creatorId)
        return FullArticleDto(article, creator)
    }

    suspend fun articleFromDto(dto: FullArticleDto): Article {
        val savedArticle = findByKeyOrNull(dto.key)
        val creatorId = dto.creator?.id ?: throw InvalidArticleRequestException("No creator for article specified")

        if (savedArticle != null && dto.id == null) throw ArticleKeyExistsException("An article with the key ${dto.key} already exists")

        val trusted = savedArticle?.trusted ?: false
        return Article(dto.id, dto.key, creatorId, dto.createdAt, dto.publishedAt, dto.updatedAt, dto.path, dto.state,
            dto.title, dto.summary, dto.colors, dto.image, dto.content, dto.accessType, dto.canEdit, dto.canView, trusted)
    }

    suspend fun deleteAll() {
        logger.debug { "Deleting all articles" }

        repository.deleteAll()
    }
}
