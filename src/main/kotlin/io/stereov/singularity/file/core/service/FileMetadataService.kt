package io.stereov.singularity.file.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.service.AuthenticationService
import io.stereov.singularity.content.common.content.service.ContentService
import io.stereov.singularity.file.core.repository.FileRepository
import io.stereov.singularity.file.core.model.FileMetadataDocument
import org.springframework.stereotype.Service

@Service
class FileMetadataService(
    override val repository: FileRepository,
    override val authenticationService: AuthenticationService,
) : ContentService<FileMetadataDocument> {

    override val logger = KotlinLogging.logger {}
    override val contentClass = FileMetadataDocument::class.java


}
