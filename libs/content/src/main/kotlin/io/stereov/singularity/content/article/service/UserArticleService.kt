package io.stereov.singularity.content.article.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.content.article.dto.ArticleResponse
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.core.auth.model.AccessType
import io.stereov.singularity.core.auth.service.AuthenticationService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service

@Service
class UserArticleService(
    private val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val authenticationService: AuthenticationService
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    private suspend fun getAccessCriteria(): Criteria {
        val isPublished = Criteria.where(Article::state.name).`is`(ArticleState.PUBLISHED)

        val isPublic = Criteria.where(Article::accessType.name).`is`(AccessType.PUBLIC)
        val isShared = Criteria.where(Article::accessType.name).`is`(AccessType.SHARED)
        val canView = { userId: String -> Criteria.where(Article::canView.name).`in`(userId).andOperator(isShared) }
        val isCreator = { userId: String -> Criteria.where(Article::creatorId.name).`is`(userId) }
        val userId = authenticationService.getCurrentUserIdOrNull()

        val accessCriteria = if (userId != null) {
            Criteria().orOperator(
                isPublic,
                canView(userId),
                isCreator(userId)
            )
        } else isPublic

        return Criteria().andOperator(accessCriteria, isPublished)
    }

    suspend fun getAccessibleArticles(limit: Long, afterId: String? = null): ArticleResponse {
        logger.debug { "Getting accessible articles limit=$limit${afterId?.let { " after $it" }}" }

        val query = Query()
            .with(Sort.by(Sort.Order.desc("_id")))
            .addCriteria(getAccessCriteria())
            .limit(limit.toInt())

        logger.debug { "Query: $query" }

        if (afterId != null) {
            query.addCriteria(Criteria.where(Article::_id.name).lt(afterId))
        }

        val articles = reactiveMongoTemplate.find(query, Article::class.java)
            .collectList()
            .awaitFirstOrNull() ?: emptyList()

        val remainingCount = if (articles.isNotEmpty()) {
            reactiveMongoTemplate.count(
                Query().addCriteria(getAccessCriteria())
                    .addCriteria(Criteria.where(Article::_id.name).lt(articles.last().id)),
                Article::class.java
            ).awaitFirstOrNull() ?: 0
        } else 0

        return ArticleResponse(articles.map { it.toOverviewDto() }, remainingCount)
    }
}
