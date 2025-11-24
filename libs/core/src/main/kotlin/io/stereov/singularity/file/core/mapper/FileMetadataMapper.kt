package io.stereov.singularity.file.core.mapper

import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.content.core.dto.response.ContentAccessDetailsResponse
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
     * Converts a [FileMetadataDocument], an authenticated user, and renditions map into a
     * [FileMetadataResponse] object.
     *
     * @param doc The file metadata document containing metadata details, such as access permissions,
     *            creation and update timestamps, and renditions.
     * @param authentication The authenticated user information, used to evaluate access permissions.
     * @param renditions A map of renditions where each key corresponds to a rendition identifier,
     *   and value is the associated [FileRenditionResponse].
     * @return A [FileMetadataResponse] object constructed from the provided metadata document,
     *   authentication context, and rendition map.
     */
    fun toMetadataResponse(
        doc: FileMetadataDocument,
        authentication: AuthenticationOutcome.Authenticated,
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
