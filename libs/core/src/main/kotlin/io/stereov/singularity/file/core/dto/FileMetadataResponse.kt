package io.stereov.singularity.file.core.dto

import io.stereov.singularity.content.core.dto.response.ContentAccessDetailsResponse
import io.stereov.singularity.content.core.dto.response.ContentResponse
import io.stereov.singularity.file.core.model.FileMetadataDocument
import org.bson.types.ObjectId
import java.time.Instant

data class FileMetadataResponse(
    val id: ObjectId,
    val key: String,
    val createdAt: Instant = Instant.now(),
    var updatedAt: Instant = Instant.now(),
    var access: ContentAccessDetailsResponse,
    val renditions: Map<String, FileRenditionResponse>,
) : ContentResponse<FileMetadataDocument> {

    val originalUrl: String?
        get() = renditions["original"]?.url
}
