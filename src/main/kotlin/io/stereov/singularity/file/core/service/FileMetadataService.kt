package io.stereov.singularity.file.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.core.component.AccessCriteria
import io.stereov.singularity.content.core.service.ContentService
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.core.repository.FileMetadataRepository
import io.stereov.singularity.global.util.CriteriaBuilder
import io.stereov.singularity.translate.service.TranslateService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
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
            .fieldContains(FileMetadataDocument::_contentType, contentType)
            .isIn(FileMetadataDocument::tags, tags)
            .build()

        return findAllPaginated(pageable, criteria)
    }
}
