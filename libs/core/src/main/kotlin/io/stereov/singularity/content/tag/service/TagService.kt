package io.stereov.singularity.content.tag.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.onFailure
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.content.core.properties.ContentProperties
import io.stereov.singularity.content.tag.dto.CreateTagMultiLangRequest
import io.stereov.singularity.content.tag.dto.CreateTagRequest
import io.stereov.singularity.content.tag.dto.UpdateTagRequest
import io.stereov.singularity.content.tag.exception.CreateTagException
import io.stereov.singularity.content.tag.exception.UpdateTagException
import io.stereov.singularity.content.tag.mapper.TagMapper
import io.stereov.singularity.content.tag.model.TagDocument
import io.stereov.singularity.content.tag.model.TagTranslation
import io.stereov.singularity.content.tag.repository.TagRepository
import io.stereov.singularity.database.core.exception.FindAllDocumentsPaginatedException
import io.stereov.singularity.database.core.exception.FindDocumentByKeyException
import io.stereov.singularity.database.core.service.CrudServiceWithKey
import io.stereov.singularity.database.core.util.CriteriaBuilder
import io.stereov.singularity.global.properties.AppProperties
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.runBlocking
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.stereotype.Service
import java.util.*

/**
 * Service class responsible for managing tags, including creating, updating, and retrieving tag documents.
 *
 * This service provides functionality for handling multilingual tags, pagination, and managing tag-related operations
 * in the application. It utilizes reactive MongoDB templates and a repository for persistence, along with property
 * configurations for content and application settings.
 */
@Service
class TagService(
    override val repository: TagRepository,
    override val reactiveMongoTemplate: ReactiveMongoTemplate,
    private val contentProperties: ContentProperties,
    private val tagMapper: TagMapper,
    private val appProperties: AppProperties,
) : CrudServiceWithKey<TagDocument> {

    override val collectionClazz = TagDocument::class.java

    @PostConstruct
    fun initializeTags() = runBlocking {
        logger.info { "Creating initial tags" }

        contentProperties.tags?.forEach { tagRequest ->
            logger.info { "Created tag with key \"${tagRequest.key}\""}
            create(tagRequest)
                .onFailure { ex -> when (ex) {
                    is CreateTagException.KeyExists -> logger.info { "Skipping creation of tag with key \"${tagRequest.key}\" because it already exists"}
                    is CreateTagException.Database -> logger.error(ex) { "Failed to create tag with key \"${tagRequest.key}\""}
                }
            }
        }
    }

    override val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Creates a new tag based on the provided request.
     *
     * @param req the request object containing the details for creating the tag
     * @return a [Result] containing the created [TagDocument] if successful; otherwise, a [CreateTagException] indicating the error that occurred
     */
    suspend fun create(
        req: CreateTagRequest
    ): Result<TagDocument, CreateTagException> = coroutineBinding {
        logger.debug { "Creating tag with key ${req.key}" }

        val exists = existsByKey(req.key)
            .mapError { ex -> CreateTagException.Database("Failed to check existence of tag with key ${req.key}: ${ex.message}", ex) }
            .bind()

        if (exists) {
            Err(CreateTagException.KeyExists("Failed to create tag: tag with key ${req.key} already exists"))
                .bind()
        }

        save(tagMapper.createTag(req))
            .mapError { ex -> CreateTagException.Database("Failed to save tag: ${ex.message}", ex) }
            .bind()
    }

    /**
     * Creates a new tag with multilingual support based on the provided request.
     *
     * @param req the request object containing the details for creating the tag, including multilingual data
     * @return a [Result] containing the created [TagDocument] if successful; otherwise, a [CreateTagException] indicating the error that occurred
     */
    suspend fun create(
        req: CreateTagMultiLangRequest
    ): Result<TagDocument, CreateTagException> = coroutineBinding {
        logger.debug { "Creating tag with key \"${req.key}\"" }

        val exists = existsByKey(req.key)
            .mapError { ex -> CreateTagException.Database("Failed to check existence of tag with key ${req.key}: ${ex.message}", ex) }
            .bind()

        if (exists) {
            Err(CreateTagException.KeyExists("Failed to create tag: tag with key ${req.key} already exists"))
                .bind()
        }

        save(tagMapper.createTag(req))
            .mapError { ex -> CreateTagException.Database("Failed to save tag: ${ex.message}", ex) }
            .bind()
    }

    /**
     * Finds all tags matching the given filters and pagination criteria.
     *
     * @param pageable the pagination information including page number, size, and sorting criteria
     * @param key an optional filter to match tags by their key
     * @param name an optional filter to match tags by their name
     * @param description an optional filter to match tags by their description
     * @param locale an optional locale for filtering multilingual tag translations; if null, a default locale will be used
     * @return a [Result] containing a paginated list of [TagDocument] objects if successful; otherwise, a [FindAllDocumentsPaginatedException] indicating the error that occurred
     *
     */
    suspend fun findAllPaginated(
        pageable: Pageable,
        key: String?,
        name: String?,
        description: String?,
        locale: Locale?
    ): Result<Page<TagDocument>, FindAllDocumentsPaginatedException> {
        logger.debug { "Finding tags" }

        val actualLocale = locale ?: appProperties.locale
        val criteria = CriteriaBuilder()
            .fieldContains(TagDocument::key, key)
            .fieldContains(TagTranslation::name, name, actualLocale)
            .fieldContains(TagTranslation::description, description, actualLocale)
            .build()

        return findAllPaginated(pageable, criteria)
    }

    /**
     * Updates an existing tag identified by the given key with the provided details in the request.
     *
     * @param key The unique key used to identify the tag to update.
     * @param req The request object containing the updated tag details, such as name, description, and locale.
     * @return A [Result] containing the updated [TagDocument] if successful; otherwise, a [UpdateTagException] indicating the error that occurred.
     */
    suspend fun updateTag(
        key: String,
        req: UpdateTagRequest
    ): Result<TagDocument, UpdateTagException> = coroutineBinding {
        logger.debug { "Updating tag with key \"$key\"" }

        val tag = findByKey(key)
            .mapError { ex -> when (ex) {
                is FindDocumentByKeyException.NotFound -> UpdateTagException.NotFound("No tag with key \"$key\" found")
                is FindDocumentByKeyException.Database -> UpdateTagException.Database("Failed to find tag with key $key: ${ex.message}", ex)
            }}
            .bind()

        val updatedTranslations = mutableMapOf<Locale, TagTranslation>()
        val actualLocale = req.locale ?: appProperties.locale

        val existing = tag.translations[actualLocale]

        if (existing != null) {
            updatedTranslations[actualLocale] = TagTranslation(
                req.name ?: existing.name,
                req.description ?: existing.description
            )
        } else {
            val name = req.name
                ?: Err(UpdateTagException.InvalidRequest("Failed to update tag \"$key\": tag name not specified"))
                    .bind()

            updatedTranslations[actualLocale] = TagTranslation(
                name,
                req.description ?: ""
            )
        }

        tag.translations.putAll(updatedTranslations)

        save(tag)
            .mapError { ex -> UpdateTagException.Database("Failed to save updated tag to database: ${ex.message}", ex) }
            .bind()
    }
}
