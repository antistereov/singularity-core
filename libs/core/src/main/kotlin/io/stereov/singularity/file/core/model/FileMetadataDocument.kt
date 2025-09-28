package io.stereov.singularity.file.core.model

import io.stereov.singularity.auth.core.model.token.AccessType
import io.stereov.singularity.content.core.model.ContentAccessDetails
import io.stereov.singularity.content.core.model.ContentDocument
import io.stereov.singularity.global.exception.model.InvalidDocumentException
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "files")
data class FileMetadataDocument(
    @Id private val _id: ObjectId? = null,
    @Indexed(unique = true) override val key: String,
    override val createdAt: Instant = Instant.now(),
    override var updatedAt: Instant = Instant.now(),
    override var access: ContentAccessDetails,
    var renditions: Map<String, FileRendition> = emptyMap(),
    var renditionKeys: Set<String>,
    var contentTypes: Set<String>,
    override var trusted: Boolean = false,
    override var tags: MutableSet<String> = mutableSetOf()
) : ContentDocument<FileMetadataDocument> {

    @get:Transient
    override val id: ObjectId
        get() = _id ?: throw InvalidDocumentException("No ID found")

    constructor(
        id: ObjectId? = null,
        key: String,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now(),
        ownerId: ObjectId,
        isPublic: Boolean,
        renditions: Map<String, FileRendition>,
        trusted: Boolean = false,
        tags: MutableSet<String> = mutableSetOf()
    ): this(
        _id = id,
        key = key,
        createdAt = createdAt,
        updatedAt = updatedAt,
        access = ContentAccessDetails(ownerId = ownerId, visibility = if (isPublic) AccessType.PUBLIC else AccessType.PRIVATE),
        renditions = renditions,
        renditionKeys = renditions.values.map { it.key }.toSet(),
        contentTypes = renditions.values.map { it.contentType }.toSet(),
        trusted = trusted,
        tags = tags
    )

    companion object {
        const val ORIGINAL_RENDITION = "original"
        const val CONTENT_TYPE = "files"
    }
}
