package io.stereov.singularity.file.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.core.service.ContentService
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.core.repository.FileMetadataRepository
import org.springframework.stereotype.Service

@Service
class FileMetadataService(
    override val repository: FileMetadataRepository,
    override val authorizationService: AuthorizationService,
) : ContentService<FileMetadataDocument> {

    override val logger = KotlinLogging.logger {}
    override val contentClass = FileMetadataDocument::class.java


}
