package io.stereov.singularity.file.core.mapper

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapError
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.content.core.dto.response.ContentAccessDetailsResponse
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.dto.FileRenditionResponse
import io.stereov.singularity.file.core.exception.FileException
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.core.model.FileRendition
import io.stereov.singularity.file.core.model.FileRenditionKey
import io.stereov.singularity.file.core.model.FileUploadResponse
import io.stereov.singularity.global.properties.AppProperties
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
class FileMetadataMapper(
    private val appProperties: AppProperties
) {

    private fun getRenditionUrl(rendition: FileRenditionKey): String {
        return "${appProperties.baseUri}/api/files/$rendition"
    }

    /**
     * Maps a given [FileRendition] instance and a URL to a [FileRenditionResponse] object.
     *
     * @param rendition The source [FileRendition] containing the file's metadata such as key, size,
     * content type, and optional dimensions.
     * @param url The URL associated with the file rendition to be included in the response.
     * @return A [FileRenditionResponse] containing the combined metadata and URL.
     */
    private fun toRenditionResponse(rendition: FileRendition, url: String) = FileRenditionResponse(
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
     * Creates a metadata response for a given file document and authentication token.
     *
     * This method maps the renditions of the provided file document to their
     * corresponding rendition responses, generates rendition URLs, and constructs
     * a comprehensive metadata response.
     *
     * @param authenticationOutcome The authentication token of the user requesting the metadata response.
     * @param doc The file metadata document containing the file's metadata and renditions.
     * @return A [Result] containing the [FileMetadataResponse] representing the metadata,
     * or a [FileException] if an error occurs during the response creation process.
     */
    fun toMetadataResponse(
        doc: FileMetadataDocument,
        authenticationOutcome: AuthenticationOutcome,
    ): Result<FileMetadataResponse, FileException> = binding {
        val fileId = doc.id
            .mapError { FileException.from(it) }
            .bind()
        val renditions = doc.renditions.map { (id, rend) ->
            id to toRenditionResponse(rend, getRenditionUrl(rend.key))
        }.toMap()

        FileMetadataResponse(
            id = fileId,
            key = doc.key,
            createdAt = doc.createdAt,
            updatedAt = doc.updatedAt,
            access = ContentAccessDetailsResponse.create(doc.access, authenticationOutcome),
            renditions = renditions,
        )
    }
}
