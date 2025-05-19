package io.stereov.singularity.core.global.util

import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query

suspend fun <T> paginateWithQuery(
    reactiveMongoTemplate: ReactiveMongoTemplate,
    query: Query,
    pageable: Pageable,
    collectionClass: Class<T>,
): Page<T> {

    val totalCount = reactiveMongoTemplate.count(query, collectionClass)
        .awaitFirstOrNull() ?: 0

    query.with(pageable)

    val content = reactiveMongoTemplate.find(query, collectionClass)
        .collectList()
        .awaitFirstOrNull()
        ?: emptyList()

    return PageImpl(content, pageable, totalCount)
}

fun getFieldContainsCriteria(field: String, substring: String): Criteria {
    val regexPattern = ".*${Regex.escape(substring)}.*"
    return Criteria.where(field).regex(regexPattern, "i")
}
