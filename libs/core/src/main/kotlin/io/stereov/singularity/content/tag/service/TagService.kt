package io.stereov.singularity.content.tag.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.content.core.properties.ContentProperties
import io.stereov.singularity.content.tag.dto.CreateTagMultiLangRequest
import io.stereov.singularity.content.tag.dto.CreateTagRequest
import io.stereov.singularity.content.tag.dto.UpdateTagRequest
import io.stereov.singularity.content.tag.exception.model.InvalidUpdateTagRequest
import io.stereov.singularity.content.tag.exception.model.TagKeyExistsException
import io.stereov.singularity.content.tag.mapper.TagMapper
import io.stereov.singularity.content.tag.model.TagDocument
import io.stereov.singularity.content.tag.model.TagTranslation
import io.stereov.singularity.content.tag.repository.TagRepository
import io.stereov.singularity.database.core.service.CrudService
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.util.CriteriaBuilder
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Service
import java.util.*

@Service
class TagService(
    override val repository: TagRepository,
    override val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val contentProperties: ContentProperties,
    private val tagMapper: TagMapper,
    private val appProperties: AppProperties,
) : CrudService<TagDocument> {

    override val collectionClazz = TagDocument::class.java

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

    override val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun create(req: CreateTagRequest): TagDocument {
        logger.debug { "Creating tag with key ${req.key}" }
        if (existsByKey(req.key)) throw TagKeyExistsException(req.key)

        return save(tagMapper.createTag(req))
    }

    suspend fun create(req: CreateTagMultiLangRequest): TagDocument {
        logger.debug { "Creating tag with key \"${req.key}\"" }

        if (existsByKey(req.key)) throw TagKeyExistsException(req.key)

        return save(tagMapper.createTag(req))
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

    suspend fun findAllPaginated(
        pageable: Pageable,
        key: String?,
        name: String?,
        description: String?,
        locale: Locale?
    ): Page<TagDocument> {
        logger.debug { "Finding tags" }

        val actualLocale = locale ?: appProperties.locale
        val criteria = CriteriaBuilder()
            .fieldContains(TagDocument::key, key)
            .fieldContains(TagTranslation::name, name, actualLocale)
            .fieldContains(TagTranslation::description, description, actualLocale)
            .build()

        return findAllPaginated(pageable, criteria)
    }

    suspend fun updateTag(key: String, req: UpdateTagRequest): TagDocument {
        logger.debug { "Updating tag with key \"$key\"" }

        val tag = findByKey(key)
        val updatedTranslations = mutableMapOf<Locale, TagTranslation>()

        req.translations.forEach { (locale, updateReq) ->
            val existing = tag.translations[locale]

            if (existing != null) {
                updatedTranslations[locale] = TagTranslation(
                    updateReq.name ?: existing.name,
                    updateReq.description ?: existing.description
                )
            } else {
                updatedTranslations[locale] = TagTranslation(
                    updateReq.name
                        ?: throw InvalidUpdateTagRequest("Failed to add new translation $locale for tag \"$key\": tag name not specified"),
                    updateReq.description ?: ""
                )
            }
        }

        tag.translations.putAll(updatedTranslations)

        return save(tag)
    }

    suspend fun deleteByKey(key: String) {
        logger.debug { "Deleting tag with key \"$key\"" }

        if (!existsByKey(key)) throw DocumentNotFoundException("No tag with key $key found")
        repository.deleteByKey(key)
    }
}
