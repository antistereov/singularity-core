package io.stereov.singularity.content.common.tag.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.content.common.tag.dto.CreateTagRequest
import io.stereov.singularity.content.common.tag.dto.UpdateTagRequest
import io.stereov.singularity.content.common.tag.exception.model.TagNameExistsException
import io.stereov.singularity.content.common.tag.model.TagDocument
import io.stereov.singularity.content.common.tag.repository.TagRepository
import io.stereov.singularity.core.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.core.global.util.getFieldContainsCriteria
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service

@Service
class TagService(
    private val repository: TagRepository,
    private val reactiveMongoTemplate: ReactiveMongoTemplate,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun create(req: CreateTagRequest): TagDocument {
        logger.debug { "Creating tag with name ${req.name}" }

        if (existsByName(req.name)) throw TagNameExistsException(req.name)

        return save(TagDocument(req))
    }

    suspend fun save(tag: TagDocument): TagDocument {
        logger.debug { "Saving tag with name \"${tag.name}\"" }

        return repository.save(tag)
    }

    suspend fun existsByName(name: String): Boolean {
        logger.debug { "Checking if there is a tag with name \"$name\" already" }

        return repository.existsByName(name)
    }

    suspend fun findById(id: ObjectId): TagDocument {
        logger.debug { "Fining tag with id \"id\"" }

        return repository.findById(id) ?: throw DocumentNotFoundException("No tag with id \"$id\" found")
    }

    suspend fun findNameContains(substring: String): List<TagDocument> {
        logger.debug { "Finding tags with name containing \"$substring\"" }

        val criteria = getFieldContainsCriteria(TagDocument::name.name, substring)

        return reactiveMongoTemplate.find<TagDocument>(Query.query(criteria))
            .collectList()
            .awaitFirstOrNull()
            ?: emptyList()
    }

    suspend fun updateTag(id: String, req: UpdateTagRequest): TagDocument {
        logger.debug { "Updating tag with id \"$id\"" }

        val tag = findById(ObjectId(id))

        tag.name = req.name ?: tag.name
        tag.description = req.description ?: tag.description

        return save(tag)
    }

    suspend fun deleteById(id: String): Boolean {
        logger.debug { "Deleting tag with id \"$id\"" }

        repository.deleteById(ObjectId(id))
        return true
    }
}
