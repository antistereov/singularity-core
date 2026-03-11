package io.stereov.singularity.file.core.model

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.token.model.AccessType
import io.stereov.singularity.content.core.model.ContentAccessDetails
import io.stereov.singularity.content.core.model.ContentDocument
import io.stereov.singularity.file.core.exception.FileException
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

/**
 * A document representation for storing metadata related to files.
 *
 * This class is used to model metadata information and properties of files stored in the system.
 * It includes details such as renditions, associated content types, access settings, and additional
 * metadata tags for the file.
 *
 * @property key The unique key identifying the file.
 * @property createdAt The timestamp indicating when the file metadata was created.
 * @property updatedAt The timestamp indicating the last update time of the file metadata.
 * @property access Details regarding access permissions and visibility of the file.
 * @property renditions A map of file renditions, where each rendition provides detail about a specific
 *     version or format of the file.
 * @property renditionKeys A set of all keys associated with the available renditions.
 * @property contentTypes A set of MIME content types represented across the renditions of the file.
 * @property trusted Indicates whether the file is trusted.
 * @property tags A mutable set of user-defined tags associated with the file for categorization or
 *     metadata purposes.
 * @property id The unique identifier (ObjectId) for the file metadata document.
 */
@Document(collection = "files")
data class FileMetadataDocument(
    @Id override val _id: ObjectId? = null,
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

    @Transient
    private val logger = KotlinLogging.logger {}

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

    /**
     * Retrieves the best matching file rendition based on the provided key.
     *
     * If a key is provided, it attempts to find a matching rendition associated with that key.
     * If no matching rendition is found using the provided key, or if no key is specified,
     * the method returns the original rendition (if available) or the first available rendition.
     * If no renditions exist, a [FileException.NotFound] is returned.
     *
     * @param key An optional key used to identify a specific file rendition.
     *            If null, the method attempts to return the original rendition or
     *            the first available rendition.
     * @return A [Result] containing the matching rendition of type [FileRendition]
     *         if found, or a [FileException.NotFound] if no matching rendition exists.
     */
    @Transient
    fun getBestMatchingRendition(key: String? = null): Result<FileRendition, FileException.NotFound> {
        logger.debug { "Finding best matching rendition for key '$key'" }
        val matchingRendition = key?.let { renditions[it] }

        if (matchingRendition != null) return Ok(matchingRendition)

        return (renditions[ORIGINAL_RENDITION] ?: renditions.values.firstOrNull())
            .toResultOr { FileException.NotFound("No rendition with key '$key' found for file with key '${this.key}'") }
    }

    companion object {
        const val ORIGINAL_RENDITION = "original"
        const val CONTENT_TYPE = "files"
    }
}
