package io.stereov.singularity.content.core.tag.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.content.translate.model.Language
import io.stereov.singularity.content.core.tag.dto.CreateTagMultiLangRequest
import io.stereov.singularity.content.core.tag.dto.CreateTagRequest
import io.stereov.singularity.content.core.tag.dto.UpdateTagRequest
import io.stereov.singularity.content.core.tag.exception.model.InvalidUpdateTagRequest
import io.stereov.singularity.content.core.tag.exception.model.TagKeyExistsException
import io.stereov.singularity.content.core.tag.model.TagDocument
import io.stereov.singularity.content.core.tag.model.TagTranslation
import io.stereov.singularity.content.core.tag.repository.TagRepository
import io.stereov.singularity.content.core.properties.ContentProperties
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.global.util.getFieldContainsCriteria
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service

@Service
class TagService(
    private val repository: TagRepository,
    private val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val contentProperties: ContentProperties,
) {

    @PostConstruct
    fun initializeTags() {
        logger.info { "Creating initial tags" }

        contentProperties.tags?.forEach { tagRequest ->
            try {
                runBlocking { create(tagRequest) }
                logger.info { "Created tag with key \"${tagRequest.key}\""}
            } catch (_: TagKeyExistsException) {
                logger.info { "Skipping creation of tag with key \"${tagRequest.key}\" because it already exists"}
            }
        }
    }

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun create(req: CreateTagRequest): TagDocument {
        logger.debug { "Creating tag with key ${req.key}" }

        if (existsByKey(req.key)) throw TagKeyExistsException(req.key)

        return save(TagDocument(req))
    }

    suspend fun create(req: CreateTagMultiLangRequest): TagDocument {
        logger.debug { "Creating tag with key \"${req.key}\"" }

        if (existsByKey(req.key)) throw TagKeyExistsException(req.key)

        return save(TagDocument(req))
    }

    suspend fun save(tag: TagDocument): TagDocument {
        logger.debug { "Saving tag with key \"${tag.key}\"" }

        return repository.save(tag)
    }

    suspend fun findByKey(key: String): TagDocument {
        logger.debug { "Finding tag by key \"$key\"" }

        return repository.findByKey(key)
            ?: throw DocumentNotFoundException("No tag with key \"$key\" found")
    }

    suspend fun existsByKey(name: String): Boolean {
        logger.debug { "Checking if there is a tag with key \"$name\" already" }

        return repository.existsByKey(name)
    }

    suspend fun findByNameContains(substring: String, lang: Language): List<TagDocument> {
        logger.debug { "Finding tags with name containing \"$substring\"" }

        val field = "${TagDocument::translations.name}.$lang.${TagTranslation::name.name}"

        val criteria = getFieldContainsCriteria(field, substring)

        return reactiveMongoTemplate.find<TagDocument>(Query.query(criteria))
            .collectList()
            .awaitFirstOrNull()
            ?: emptyList()
    }

    suspend fun updateTag(key: String, req: UpdateTagRequest): TagDocument {
        logger.debug { "Updating tag with key \"$key\"" }

        val tag = findByKey(key)
        val updatedTranslations = mutableMapOf<Language, TagTranslation>()

        req.translations.forEach { (lang, updateReq) ->
            val existing = tag.translations[lang]

            if (existing != null) {
                updatedTranslations.put(
                    lang,
                    TagTranslation(
                        updateReq.name ?: existing.name,
                        updateReq.description ?: existing.description
                    )
                )
            } else {
                updatedTranslations.put(
                    lang,
                    TagTranslation(
                        updateReq.name
                            ?: throw InvalidUpdateTagRequest("Failed to add new translation $lang for tag \"$key\": tag name not specified"),
                        updateReq.description ?: ""
                    )
                )
            }
        }

        tag.translations.putAll(updatedTranslations)

        return save(tag)
    }

    suspend fun deleteByKey(key: String): Boolean {
        logger.debug { "Deleting tag with key \"$key\"" }

        return repository.deleteByKey(key)
    }

    suspend fun deleteAll() {
        logger.debug { "Deleting all tags" }

        repository.deleteAll()
    }
}
