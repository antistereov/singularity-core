package io.stereov.singularity.stereovio.content.article.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.core.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.core.user.service.UserService
import io.stereov.singularity.stereovio.content.article.dto.FullArticleDto
import io.stereov.singularity.stereovio.content.article.model.Article
import io.stereov.singularity.stereovio.content.article.repository.ArticleRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service

@Service
class ArticleService(
    private val repository: ArticleRepository,
    private val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val userService: UserService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun save(article: Article): Article {
        logger.debug { "Saving article" }

        return repository.save(article)
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

    suspend fun getLatestArticles(limit: Long): List<Article> {
        val query = Query()
            .with(Sort.by(Sort.Order.desc("_id")))
            .limit(limit.toInt())

        return reactiveMongoTemplate.find(query, Article::class.java)
            .collectList()
            .awaitFirstOrNull() ?: emptyList()
    }

    suspend fun getNextArticles(lastLoadedId: String, limit: Long): List<Article> {
        return repository.findByIdLessThanOrderByIdDesc(lastLoadedId, limit).toList()
    }

    suspend fun fullArticledDtoFrom(article: Article): FullArticleDto {
        val creator = userService.findById(article.creatorId)
        return FullArticleDto(article, creator)
    }
}
