package io.stereov.singularity.content.article.queries

import io.stereov.singularity.content.common.content.util.AccessCriteria
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.stereotype.Component


@Component
class ArticleQueries(
    private val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val converter: MappingMongoConverter,
    private val accessCriteria: AccessCriteria,
) {

}
