package io.stereov.singularity.database.core.service

import io.github.oshai.kotlinlogging.KLogger
import kotlinx.coroutines.reactive.awaitFirstOrElse
import org.springframework.data.domain.*
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

interface CrudService<T: Any> {

    val logger: KLogger

    val collectionClass: Class<T>
    val reactiveMongoTemplate: ReactiveMongoTemplate

    suspend fun findAllPaginated(page: Int, size: Int, sort: List<String>, criteria: Criteria? = null): Page<T> {

        val pageable = PageRequest.of(page, size, Sort.by(sort.map { item ->
            val (property, direction) = item.split(",")
            Sort.Order(Sort.Direction.fromString(direction), property)
        }))

        return findAllPaginated(pageable, criteria)
    }

    suspend fun findAllPaginated(pageable: Pageable, criteria: Criteria? = null): Page<T> {
        logger.debug { "Finding ${collectionClass.simpleName}: page ${pageable.pageNumber}, size: ${pageable.pageSize}, sort: ${pageable.sort}" }


        val query = criteria?.let { Query(it) } ?: Query()
        val paginatedQuery = query.with(pageable)

        val count = reactiveMongoTemplate.count(paginatedQuery, collectionClass).awaitFirstOrElse { 0 }
        val groups = reactiveMongoTemplate.find(paginatedQuery, collectionClass).collectList().awaitFirstOrElse { emptyList() }

        return PageImpl(groups, pageable, count)
    }
}
