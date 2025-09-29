package io.stereov.singularity.file.core.dto

import io.stereov.singularity.content.core.dto.response.ContentResponse
import io.stereov.singularity.content.core.model.ContentAccessDetails
import io.stereov.singularity.content.core.model.ContentDocument
import io.stereov.singularity.file.core.model.FileMetadataDocument
import org.bson.types.ObjectId
import java.time.Instant

data class FileMetadataResponse(
    override val id: ObjectId,
    override val key: String,
    override val createdAt: Instant = Instant.now(),
    override var updatedAt: Instant = Instant.now(),
    override var access: ContentAccessDetails,
    val renditions: Map<String, FileRenditionResponse>,
    override var trusted: Boolean,
    override var tags: MutableSet<String>
) : ContentDocument<FileMetadataResponse>, ContentResponse<FileMetadataDocument>
