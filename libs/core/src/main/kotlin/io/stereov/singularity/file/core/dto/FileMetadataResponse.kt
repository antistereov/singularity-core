package io.stereov.singularity.file.core.dto

import io.stereov.singularity.content.common.content.model.ContentAccessDetails
import io.stereov.singularity.content.common.content.model.ContentDocument
import org.bson.types.ObjectId
import java.time.Instant

data class FileMetadataResponse(
    override val id: ObjectId,
    override val key: String,
    override val createdAt: Instant = Instant.now(),
    override var updatedAt: Instant = Instant.now(),
    override var access: ContentAccessDetails,
    val contentType: String,
    val url: String,
    val size: Long,
    override val trusted: Boolean = false,
    override var tags: MutableSet<String> = mutableSetOf()
) : ContentDocument<FileMetadataResponse>
