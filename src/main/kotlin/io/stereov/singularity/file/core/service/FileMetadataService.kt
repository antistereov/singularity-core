package io.stereov.singularity.file.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.core.component.AccessCriteria
import io.stereov.singularity.content.core.service.ContentService
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.core.repository.FileMetadataRepository
import io.stereov.singularity.global.exception.model.DocumentNotFoundException
import io.stereov.singularity.global.util.CriteriaBuilder
import io.stereov.singularity.translate.service.TranslateService
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.exists
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class FileMetadataService(
    override val repository: FileMetadataRepository,
    override val authorizationService: AuthorizationService,
    override val reactiveMongoTemplate: ReactiveMongoTemplate,
    override val translateService: TranslateService,
    override val accessCriteria: AccessCriteria
) : ContentService<FileMetadataDocument>() {

    override val logger = KotlinLogging.logger {}
    override val collectionClazz = FileMetadataDocument::class.java

    private suspend fun renditionQuery(key: String, authorized: Boolean = false): Query {
        val criteria = CriteriaBuilder()
            .hasElement(FileMetadataDocument::renditionKeys, key)
        if (authorized) criteria.add(accessCriteria.getAccessCriteria())
        return criteria.query()
    }

    suspend fun findRenditionByKeyOrNull(key: String): FileMetadataDocument? {
        logger.debug { "Finding file with key $key" }
        return reactiveMongoTemplate.find<FileMetadataDocument>(renditionQuery(key))
            .awaitFirstOrNull()
    }
    suspend fun findRenditionByKey(key: String): FileMetadataDocument {
        return findRenditionByKeyOrNull(key)
            ?: throw DocumentNotFoundException("No file with key $key found")
    }

    suspend fun existsFileByKey(key: String): Boolean {
        logger.debug { "Checking existence of file with key $key" }
        return reactiveMongoTemplate.exists<FileMetadataDocument>(renditionQuery(key))
            .awaitFirst()
    }

    suspend fun deleteRenditionByKey(key: String) {
        logger.debug { "Deleting file with key $key" }

        val metadata = findRenditionByKey(key)

        if (metadata.renditionKeys.size == 1) {
            logger.debug { "Deleting metadata document because the only rendition is deleted" }
            deleteByKey(metadata.key)
            return
        }

        metadata.renditions = metadata.renditions.filter { (_,v) -> v.key != key }
        save(metadata)
    }

    override suspend fun save(doc: FileMetadataDocument): FileMetadataDocument {
        doc.renditionKeys = doc.renditions.values.map { it.key }.toSet()
        doc.contentTypes = doc.renditions.values.map { it.contentType }.toSet()

        return super.save(doc)
    }

    suspend fun getFiles(
        pageable: Pageable,
        tags: List<String>,
        roles: Set<String>,
        contentType: String?,
        createdAtBefore: Instant?,
        createdAtAfter: Instant?,
        updatedAtBefore: Instant?,
        updatedAtAfter: Instant?,
    ): Page<FileMetadataDocument> {

        val criteria = CriteriaBuilder(accessCriteria.getAccessCriteria(roles))
            .compare(FileMetadataDocument::createdAt, createdAtBefore, createdAtAfter)
            .compare(FileMetadataDocument::updatedAt, updatedAtBefore, updatedAtAfter)
            .hasElement(FileMetadataDocument::contentTypes, contentType)
            .isIn(FileMetadataDocument::tags, tags)
            .build()

        return findAllPaginated(pageable, criteria)
    }
}
