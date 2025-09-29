package io.stereov.singularity.file.core.mapper

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

    fun toMetadataResponse(doc: FileMetadataDocument, renditions: Map<String, FileRenditionResponse>): FileMetadataResponse {

        return FileMetadataResponse(
            id = doc.id,
            key = doc.key,
            createdAt = doc.createdAt,
            updatedAt = doc.updatedAt,
            access = doc.access,
            renditions = renditions,
            trusted = doc.trusted,
            tags = doc.tags
        )
    }

    fun toDocument(res: FileMetadataResponse): FileMetadataDocument {
        return FileMetadataDocument(
            _id = res.id,
            key = res.key,
            createdAt = res.createdAt,
            updatedAt = res.updatedAt,
            access = res.access,
            renditions = res.renditions.map { (key, value) ->
                key to FileRendition(
                    value.key,
                    value.size,
                    value.contentType,
                    value.height,
                    value.width
                )
            }.toMap(),
            renditionKeys = res.renditions.keys,
            contentTypes = res.renditions.values.map { it.contentType }.toSet(),
            trusted = res.trusted,
            tags = res.tags
        )
    }
}