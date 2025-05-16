package io.stereov.singularity.content.article.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.content.article.dto.ArticleResponse
import io.stereov.singularity.content.article.model.Article
import io.stereov.singularity.content.article.model.ArticleState
import io.stereov.singularity.content.common.service.ContentManagementService
import io.stereov.singularity.content.common.util.AccessCriteria
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service

@Service
class ArticleManagementService(
    reactiveMongoTemplate: ReactiveMongoTemplate,
    accessCriteria: AccessCriteria,
) : ContentManagementService<Article>(reactiveMongoTemplate, accessCriteria, Article::class.java) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    private val isPublished = Criteria.where(Article::state.name).`is`(ArticleState.PUBLISHED.toString())

    suspend fun getAccessibleArticles(limit: Long, afterId: String? = null): ArticleResponse {
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

        return ArticleResponse(articles.map { it.toOverviewResponse() }, remainingCount)
    }
}
