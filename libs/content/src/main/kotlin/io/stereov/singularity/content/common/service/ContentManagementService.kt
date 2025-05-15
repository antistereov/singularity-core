package io.stereov.singularity.content.common.service

import io.stereov.singularity.content.common.model.ContentDocument
import io.stereov.singularity.content.common.util.AccessCriteria
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

abstract class ContentManagementService<T: ContentDocument<T>>(
    protected val reactiveMongoTemplate: ReactiveMongoTemplate,
    protected val accessCriteria: AccessCriteria,
    private val collectionClass: Class<T>
) {

    suspend fun paginateWithQuery(criteria: Criteria, pageable: Pageable): Page<T> {
        val query = Query(criteria).with(pageable)

        val content = reactiveMongoTemplate.find(query, collectionClass)
            .collectList()
            .awaitFirstOrNull()
            ?: emptyList()

        val totalCount = reactiveMongoTemplate.count(Query(criteria), collectionClass)
            .awaitFirstOrNull() ?: 0

        return PageImpl(content, pageable, totalCount)
    }

    suspend fun findAccessible(pageable: Pageable): Page<T> {
        return paginateWithQuery(accessCriteria.getViewCriteria(), pageable)
    }

    suspend fun findEditable(pageable: Pageable): Page<T> {
        return paginateWithQuery(accessCriteria.getEditCriteria(), pageable)
    }

}
