package io.stereov.singularity.content.file.model

import io.stereov.singularity.auth.model.AccessType
import io.stereov.singularity.content.common.content.model.ContentAccessDetails
import io.stereov.singularity.content.common.content.model.ContentDocument
import io.stereov.singularity.global.exception.model.InvalidDocumentException
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.http.MediaType
import java.time.Instant

@Document(collection = "files")
data class FileMetadataDocument(
    @Id private val _id: ObjectId? = null,
    @Indexed(unique = true) override val key: String,
    override val createdAt: Instant = Instant.now(),
    override var updatedAt: Instant = Instant.now(),
    override var access: ContentAccessDetails,
    private val _contentType: String,
    val publicUrl: String?,
    val size: Long,
    override val trusted: Boolean = false,
    override var tags: MutableSet<String> = mutableSetOf()
) : ContentDocument<FileMetadataDocument> {

    @get:Transient
    override val id: ObjectId
        get() = _id ?: throw InvalidDocumentException("No ID found")

    @get:Transient
    val contentType: MediaType
        get() = MediaType.valueOf(_contentType)

    constructor(
        id: ObjectId? = null,
        key: String,
        createdAt: Instant = Instant.now(),
        updatedAt: Instant = Instant.now(),
        ownerId: ObjectId,
        contentType: MediaType,
        accessType: AccessType,
        publicUrl: String?,
        size: Long,
        trusted: Boolean = false,
        tags: MutableSet<String> = mutableSetOf()
    ): this(
        _id = id,
        key = key,
        createdAt = createdAt,
        updatedAt = updatedAt,
        access = ContentAccessDetails(ownerId = ownerId, visibility = accessType),
        _contentType = contentType.toString(),
        publicUrl = publicUrl,
        size = size,
        trusted = trusted,
        tags = tags
    )
}
