package io.stereov.singularity.content.common.service

import io.stereov.singularity.content.common.model.ContentDocument
import io.stereov.singularity.content.common.util.AccessCriteria
import io.stereov.singularity.core.global.util.paginateWithQuery
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

abstract class ContentManagementService<T: ContentDocument<T>>(
    protected val reactiveMongoTemplate: ReactiveMongoTemplate,
    protected val accessCriteria: AccessCriteria,
    private val collectionClass: Class<T>
) {

    suspend fun findAccessible(pageable: Pageable): Page<T> {
        return paginateWithQuery(reactiveMongoTemplate, accessCriteria.getViewCriteria(), pageable, collectionClass)
    }
}
