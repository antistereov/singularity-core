package io.stereov.singularity.file.core.mapper

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.content.core.dto.response.ContentAccessDetailsResponse
import io.stereov.singularity.database.core.exception.DatabaseException
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.dto.FileRenditionResponse
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.core.model.FileRendition
import io.stereov.singularity.file.core.model.FileUploadResponse
import org.springframework.stereotype.Component

/**
 * A component responsible for mapping metadata and information related to file renditions
 * and metadata documents into response objects or other corresponding models.
 *
 * This class contains methods to convert data from domain objects such as [FileRendition],
 * [FileUploadResponse], and [FileMetadataDocument] into corresponding response models
 * and vice versa. 
 * It is designed to simplify the process of constructing response data
 * structures for client-side consumption.
 */
@Component
class FileMetadataMapper {

    /**
     * Maps a given [FileRendition] instance and a URL to a [FileRenditionResponse] object.
     *
     * @param rendition The source [FileRendition] containing the file's metadata such as key, size,
     * content type, and optional dimensions.
     * @param url The URL associated with the file rendition to be included in the response.
     * @return A [FileRenditionResponse] containing the combined metadata and URL.
     */
    fun toRenditionResponse(rendition: FileRendition, url: String) = FileRenditionResponse(
        size = rendition.size,
        contentType = rendition.contentType,
        key = rendition.key,
        width = rendition.width,
        height = rendition.height,
        url = url,
    )

    /**
     * Converts a [FileUploadResponse] instance into a [FileRendition] object.
     *
     * @param upload The source [FileUploadResponse] containing details such as file key, size,
     * MIME type, and optional dimensions.
     * @return A [FileRendition] object constructed from the provided file upload information.
     */
    fun toRendition(upload: FileUploadResponse) = FileRendition(
        size = upload.size,
        contentType = upload.contentType,
        key = upload.key,
        width = upload.width,
        height = upload.height,
    )

    /**
     * Converts a [FileMetadataDocument] into a [FileMetadataResponse] while incorporating authentication
     * outcomes and file renditions.
     *
     * @param doc The [FileMetadataDocument] containing the metadata details to be transformed.
     * @param authenticationOutcome The outcome of the authentication process used to determine access details.
     * @param renditions A map of file rendition keys to their corresponding [FileRenditionResponse] objects.
     * @return A [Result] wrapping a [FileMetadataResponse] if the operation is successful, or a
     * [DatabaseException.InvalidDocument] if the document contains no ID.
     */
    fun toMetadataResponse(
        doc: FileMetadataDocument,
        authenticationOutcome: AuthenticationOutcome,
        renditions: Map<String, FileRenditionResponse>
    ): Result<FileMetadataResponse, DatabaseException.InvalidDocument> = binding {
        val id = doc.id.bind()

        FileMetadataResponse(
            id = id,
            key = doc.key,
            createdAt = doc.createdAt,
            updatedAt = doc.updatedAt,
            access = ContentAccessDetailsResponse.create(doc.access, authenticationOutcome),
            renditions = renditions,
        )
    }
}
