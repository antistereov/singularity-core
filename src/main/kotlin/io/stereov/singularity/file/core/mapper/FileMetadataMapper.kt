package io.stereov.singularity.file.core.mapper

import io.stereov.singularity.auth.core.model.token.AuthenticationToken
import io.stereov.singularity.content.core.dto.response.ContentAccessDetailsResponse
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.dto.FileRenditionResponse
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.core.model.FileRendition
import io.stereov.singularity.file.core.model.FileUploadResponse
import org.springframework.stereotype.Component

@Component
class FileMetadataMapper {

    fun toRenditionResponse(rendition: FileRendition, url: String) = FileRenditionResponse(
        size = rendition.size,
        contentType = rendition.contentType,
        key = rendition.key,
        width = rendition.width,
        height = rendition.height,
        url = url,
    )

    fun toRendition(upload: FileUploadResponse) = FileRendition(
        size = upload.size,
        contentType = upload.contentType,
        key = upload.key,
        width = upload.width,
        height = upload.height,
    )

    suspend fun toMetadataResponse(
        doc: FileMetadataDocument,
        authentication: AuthenticationToken,
        renditions: Map<String, FileRenditionResponse>
    ) = FileMetadataResponse(
        id = doc.id,
        key = doc.key,
        createdAt = doc.createdAt,
        updatedAt = doc.updatedAt,
        access = ContentAccessDetailsResponse.create(doc.access, authentication),
        renditions = renditions,
    )

}