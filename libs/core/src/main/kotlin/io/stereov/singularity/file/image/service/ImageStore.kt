package io.stereov.singularity.file.image.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import com.github.michaelbull.result.coroutines.runSuspendCatching
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.Position
import com.sksamuel.scrimage.webp.WebpWriter
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.file.core.component.DataBufferPublisher
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.exception.FileException
import io.stereov.singularity.file.core.model.FileKey
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.core.model.FileUploadRequest
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.download.model.StreamedFile
import io.stereov.singularity.file.image.properties.ImageProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service

/**
 * Service for handling image uploads and processing, including resizing, format conversion, and storage in a file system.
 *
 * @property imageProperties Configuration properties for image processing, such as dimensions for various renditions.
 * @property fileStorage File storage service for handling file operations like uploads.
 * @property dataBufferPublisher Utility for converting reactive data streams to byte arrays.
 */
@Service
class ImageStore(
    private val imageProperties: ImageProperties,
    private val fileStorage: FileStorage,
    private val dataBufferPublisher: DataBufferPublisher,
    private val webpWriter: WebpWriter
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Uploads an image file to the file storage while handling authentication, metadata, and visibility.
     *
     * @param authentication The authentication outcome representing the authenticated user.
     * @param file The streamed file to be uploaded.
     * @param key The unique key used to identify the uploaded file.
     * @param isPublic Whether the uploaded file should be publicly accessible.
     * @return A [Result] containing the [FileMetadataResponse] if the upload is successful, or a [FileException] if an error occurs.
     */
    suspend fun upload(
        authentication: AuthenticationOutcome.Authenticated,
        file: StreamedFile,
        key: String,
        isPublic: Boolean
    ): Result<FileMetadataResponse, FileException> {
        logger.debug { "Uploading image with key $key" }
        val originalExtension = file.url.substringAfterLast(".", "")
        val imageBytes = dataBufferPublisher.toSingleByteArray(file.content)


        return upload(authentication, imageBytes, file.contentType, key, isPublic, originalExtension)
    }

    /**
     * Uploads a file to the storage system, handling metadata, authentication, and visibility options.
     *
     * @param authentication The authentication outcome representing the authenticated user.
     * @param file The file to be uploaded, provided as a [FilePart].
     * @param key The unique key or identifier for the uploaded file.
     * @param isPublic A boolean indicating whether the file should be publicly accessible.
     * @return A [Result] encapsulating the [FileMetadataResponse] upon successful upload, or a [FileException] in case of failure.
     */
    suspend fun upload(
        authentication: AuthenticationOutcome.Authenticated,
        file: FilePart,
        key: String,
        isPublic: Boolean
    ): Result<FileMetadataResponse, FileException> {
        val imageBytes = dataBufferPublisher.toSingleByteArray(file.content())
        val originalExtension = file.filename().substringAfterLast(".", "")

        return upload(authentication, imageBytes, file.headers().contentType, key, isPublic, originalExtension)
    }

    private suspend fun createUploadRequest(
        originalImage: ImmutableImage,
        size: Int,
        key: String,
        suffix: String?,
        mediaType: String
    ): Result<FileUploadRequest, FileException> {

        return runSuspendCatching {
            val fileKey = FileKey(filename = key, suffix = suffix, extension = "webp")
            logger.debug { "Creating upload request for $fileKey" }
            val resized = originalImage.resize(size)

            val resizedBytes = resized.bytes(webpWriter)

            FileUploadRequest.ByteArrayUpload(
                key = fileKey,
                contentType = mediaType,
                data = resizedBytes,
                width = resized.width,
                height = resized.height,
            )
        }
            .mapError { ex -> FileException.Operation("Failed to create upload request for $key: ${ex.message}", ex) }
    }

    suspend fun ImmutableImage.resize(size: Int): ImmutableImage = withContext(Dispatchers.Default) {
        val w = this@resize.width
        val h = this@resize.height
        val shortEdge = minOf(w, h)

        val scale = minOf(1.0, size.toDouble() / shortEdge.toDouble())
        val targetW = maxOf(1, (w * scale).toInt())
        val targetH = maxOf(1, (h * scale).toInt())

        val resizedImage = if (targetW != w || targetH != h) {
            this@resize.scaleTo(targetW, targetH)
        } else {
            this@resize
        }

        val targetCropWidth = resizedImage.width
        val targetCropHeight = resizedImage.height

        val isTooWide = targetCropWidth.toDouble() / targetCropHeight > MAX_ASPECT_RATIO
        val isTooTall = targetCropHeight.toDouble() / targetCropWidth > MAX_ASPECT_RATIO

        val finalImage = when {
            isTooWide -> {
                val newWidth = (targetCropHeight * MAX_ASPECT_RATIO).toInt()
                resizedImage.resizeTo(newWidth, targetCropHeight, Position.Center)
            }
            isTooTall -> {
                val newHeight = (targetCropWidth / MAX_ASPECT_RATIO).toInt()
                resizedImage.resizeTo(targetCropWidth, newHeight, Position.Center)
            }
            else -> resizedImage
        }

        return@withContext finalImage
    }

    /**
     * Uploads an image along with its renditions to a file storage system.
     *
     * @param authentication The authentication object containing user credentials and permissions.
     * @param imageBytes The raw byte array of the image to be uploaded.
     * @param contentType The media type of the image (e.g., "image/jpeg", "image/png"). Null if unspecified.
     * @param key The unique identifier for the file in the storage.
     * @param isPublic Boolean flag indicating whether the file should be publicly accessible.
     * @param fileExtension The file extension of the uploaded image (e.g., "jpg", "png"). Null if unspecified.
     * @return A [Result] containing the [FileMetadataResponse] of the uploaded file on success, or a [FileException] on failure.
     */
    suspend fun upload(
        authentication: AuthenticationOutcome.Authenticated,
        imageBytes: ByteArray,
        contentType: MediaType?,
        key: String,
        isPublic: Boolean,
        fileExtension: String?
    ): Result<FileMetadataResponse, FileException> = coroutineBinding {
        val webpMediaType = MediaType.parseMediaType("image/webp")

        val actualContentType = contentType.toResultOr {
            FileException.UnsupportedMediaType("Content type is not specified")
        }.flatMap {
            if (it in ALLOWED_MEDIA_TYPES) {
                Ok(it)
            } else {
                Err(FileException.UnsupportedMediaType("Unsupported file type: $contentType"))
            }
        }.bind()

        val originalImage = runSuspendCatching {
            withContext(Dispatchers.Default) {
                ImmutableImage.loader().fromBytes(imageBytes)
            }
        }
            .mapError { ex -> FileException.Stream("Failed to load image to image loader: ${ex.message}", ex) }
            .bind()

        val filesToUpload = mutableMapOf<String, FileUploadRequest>()

        filesToUpload[ImageProperties::small.name] = createUploadRequest(
            originalImage,
            imageProperties.small,
            key,
            ImageProperties::small.name,
            webpMediaType.toString()
        ).bind()
        filesToUpload[ImageProperties::medium.name] = createUploadRequest(
            originalImage,
            imageProperties.medium,
            key,
            ImageProperties::medium.name,
            webpMediaType.toString()
        ).bind()
        filesToUpload[ImageProperties::large.name] = createUploadRequest(
            originalImage,
            imageProperties.large,
            key,
            ImageProperties::large.name,
            webpMediaType.toString()
        ).bind()

        if (imageProperties.storeOriginal) {
            filesToUpload[FileMetadataDocument.ORIGINAL_RENDITION] = FileUploadRequest.ByteArrayUpload(
                key = FileKey(filename = key, extension = fileExtension),
                contentType = actualContentType.toString(),
                data = imageBytes,
                width = originalImage.width,
                height = originalImage.height,
            )
        }

        fileStorage.uploadMultipleRenditions(
            authentication,
            FileKey(filename = key).key,
            files = filesToUpload,
            isPublic,
        ).bind()
    }

    companion object {
        const val MAX_ASPECT_RATIO = 16.0 / 9.0
        val ALLOWED_MEDIA_TYPES = listOf(MediaType.IMAGE_JPEG, MediaType.IMAGE_GIF, MediaType.IMAGE_PNG)
    }
}
