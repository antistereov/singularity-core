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
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.core.model.FileRenditionKey
import io.stereov.singularity.file.core.model.FileUploadRequest
import io.stereov.singularity.file.core.model.WithFileStorage
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.core.util.FileKeyHelper
import io.stereov.singularity.file.core.util.mediaType
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

    suspend fun upload(
        file: StreamedFile,
        filename: String = file.url.substringAfterLast("/"),
        path: String?,
        isPublic: Boolean,
        authentication: AuthenticationOutcome.Authenticated,
    ): Result<FileMetadataResponse, FileException> {
        logger.debug { "Uploading image with from URL ${file.url}" }
        val imageBytes = dataBufferPublisher.toSingleByteArray(file.content)
        return upload(filename, imageBytes, file.contentType, path, isPublic, authentication)
    }

    suspend fun upload(
        file: StreamedFile,
        filename: String = file.url.substringAfterLast("/"),
        document: WithFileStorage,
        isPublic: Boolean,
        authentication: AuthenticationOutcome.Authenticated,
    ): Result<FileMetadataResponse, FileException> {
        logger.debug { "Uploading image with from URL ${file.url}" }
        val imageBytes = dataBufferPublisher.toSingleByteArray(file.content)
        return upload(filename, imageBytes, file.contentType, document.fileStoragePath, isPublic, authentication)
    }

    suspend fun upload(
        file: FilePart,
        filename: String = file.filename(),
        path: String?,
        isPublic: Boolean,
        authentication: AuthenticationOutcome.Authenticated,
    ): Result<FileMetadataResponse, FileException> = coroutineBinding {
        val imageBytes = dataBufferPublisher.toSingleByteArray(file.content())
        val mediaType = file.mediaType().bind()

        upload(filename, imageBytes, mediaType, path, isPublic, authentication)
            .bind()
    }

    suspend fun upload(
        file: FilePart,
        filename: String = file.filename(),
        document: WithFileStorage,
        isPublic: Boolean,
        authentication: AuthenticationOutcome.Authenticated,
    ): Result<FileMetadataResponse, FileException> = coroutineBinding {
        val imageBytes = dataBufferPublisher.toSingleByteArray(file.content())
        val mediaType = file.mediaType().bind()

        upload(filename, imageBytes, mediaType, document.fileStoragePath, isPublic, authentication)
            .bind()
    }

    private suspend fun createUploadRequest(
        originalImage: ImmutableImage,
        size: Int,
        renditionKey: FileRenditionKey,
        mediaType: MediaType
    ): Result<FileUploadRequest, FileException> {

        return runSuspendCatching {
            logger.debug { "Creating upload request for rendition with key '$renditionKey'" }
            val resized = originalImage.resize(size)

            val resizedBytes = resized.bytes(webpWriter)

            FileUploadRequest.ByteArrayUpload(
                key = renditionKey,
                mediaType = mediaType,
                data = resizedBytes,
                width = resized.width,
                height = resized.height,
            )
        }
            .mapError { ex -> FileException.Operation("Failed to create upload request for $renditionKey: ${ex.message}", ex) }
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

    suspend fun upload(
        filename: String,
        imageBytes: ByteArray,
        mediaType: MediaType?,
        path: String?,
        isPublic: Boolean,
        authentication: AuthenticationOutcome.Authenticated
    ): Result<FileMetadataResponse, FileException> = coroutineBinding {
        val webpMediaType = MediaType.parseMediaType("image/webp")

        val actualContentType = mediaType.toResultOr {
            FileException.UnsupportedMediaType("Content type is not specified")
        }.flatMap {
            if (it in ALLOWED_MEDIA_TYPES) {
                Ok(it)
            } else {
                Err(FileException.UnsupportedMediaType("Unsupported file type: $mediaType"))
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

        val fileKeyHelper = FileKeyHelper(
            filename,
            mediaType,
            path
        )

        filesToUpload[ImageProperties::small.name] = createUploadRequest(
            originalImage,
            imageProperties.small,
            fileKeyHelper.toRenditionKey(ImageProperties::small.name),
            webpMediaType
        ).bind()
        filesToUpload[ImageProperties::medium.name] = createUploadRequest(
            originalImage,
            imageProperties.medium,
            fileKeyHelper.toRenditionKey(ImageProperties::medium.name),
            webpMediaType
        ).bind()
        filesToUpload[ImageProperties::large.name] = createUploadRequest(
            originalImage,
            imageProperties.large,
            fileKeyHelper.toRenditionKey(ImageProperties::large.name),
            webpMediaType
        ).bind()

        val documentKey = fileKeyHelper.toDocumentKey()

        if (imageProperties.storeOriginal) {
            filesToUpload[FileMetadataDocument.ORIGINAL_RENDITION] = FileUploadRequest.ByteArrayUpload(
                key = fileKeyHelper.toRenditionKey(FileMetadataDocument.ORIGINAL_RENDITION),
                mediaType = actualContentType,
                data = imageBytes,
                width = originalImage.width,
                height = originalImage.height,
            )
        }

        fileStorage.uploadMultipleRenditions(
            documentKey,
            files = filesToUpload,
            isPublic,
            authentication,
        ).bind()
    }

    companion object {
        const val MAX_ASPECT_RATIO = 16.0 / 9.0
        val ALLOWED_MEDIA_TYPES = listOf(MediaType.IMAGE_JPEG, MediaType.IMAGE_GIF, MediaType.IMAGE_PNG)
    }
}
