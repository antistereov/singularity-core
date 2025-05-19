package io.stereov.singularity.content.common.content.service

import io.stereov.singularity.content.common.content.model.ContentDocument
import io.stereov.singularity.content.common.util.AccessCriteria
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

abstract class ContentManagementService<T: ContentDocument<T>>(
    protected val reactiveMongoTemplate: ReactiveMongoTemplate,
    protected val accessCriteria: AccessCriteria,
    private val collectionClass: Class<T>
) {
}
