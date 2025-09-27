package io.stereov.singularity.file.image.service

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.Position
import com.sksamuel.scrimage.webp.WebpWriter
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.file.core.component.DataBufferPublisher
import io.stereov.singularity.file.core.dto.FileMetadataResponse
import io.stereov.singularity.file.core.exception.model.FileTooLargeException
import io.stereov.singularity.file.core.exception.model.UnsupportedMediaTypeException
import io.stereov.singularity.file.core.model.FileKey
import io.stereov.singularity.file.core.model.FileMetadataDocument
import io.stereov.singularity.file.core.model.FileUploadRequest
import io.stereov.singularity.file.core.properties.StorageProperties
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.image.properties.ImageProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.types.ObjectId
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import org.springframework.stereotype.Service

@Service
class ImageStore(
    private val imageProperties: ImageProperties,
    private val webpWriter: WebpWriter,
    private val fileStorage: FileStorage,
    private val storageProperties: StorageProperties,
    private val dataBufferPublisher: DataBufferPublisher
) {

    private val logger = KotlinLogging.logger {}

    suspend fun upload(
        ownerId: ObjectId,
        file: FilePart,
        key: String,
        isPublic: Boolean,
    ): FileMetadataResponse {
        logger.debug { "Uploading image with key $key" }

        storageProperties.validateContentLength(file.headers().contentLength)

        val allowedMediaTypes = listOf(MediaType.IMAGE_JPEG, MediaType.IMAGE_GIF, MediaType.IMAGE_PNG)
        val webpMediaType = MediaType.parseMediaType("image/webp")

        val contentType = file.headers().contentType
            ?: throw UnsupportedMediaTypeException("Media type is not set")

        if (contentType !in allowedMediaTypes) {
            throw UnsupportedMediaTypeException("Unsupported file type: $contentType")
        }

        val imageBytes = dataBufferPublisher.toSingleByteArray(file.content())
        if (imageBytes.size > storageProperties.maxFileSize) {
            throw FileTooLargeException("")
        }
        val originalImage = withContext(Dispatchers.Default) {
            ImmutableImage.loader().fromBytes(imageBytes)
        }
        val originalExtension = file.filename().substringAfterLast(".", "")

        val filesToUpload = mutableMapOf<String, FileUploadRequest>()

        filesToUpload[ImageProperties::small.name] = createUploadRequest(
            originalImage,
            imageProperties.small,
            key,
            ImageProperties::small.name,
            webpMediaType.toString()
        )
        filesToUpload[ImageProperties::medium.name] = createUploadRequest(
            originalImage,
            imageProperties.medium,
            key,
            ImageProperties::medium.name,
            webpMediaType.toString()
        )
        filesToUpload[ImageProperties::large.name] = createUploadRequest(
            originalImage,
            imageProperties.large,
            key,
            ImageProperties::large.name,
            webpMediaType.toString()
        )

        if (imageProperties.storeOriginal) {
            filesToUpload[FileMetadataDocument.ORIGINAL_RENDITION] = FileUploadRequest.ByteArrayUpload(
                key = FileKey(filename = key, extension = originalExtension),
                contentType = contentType.toString(),
                data = imageBytes,
                width = originalImage.width,
                height = originalImage.height,
                contentLength = imageBytes.size.toLong()
            )
        }

        return fileStorage.uploadMultipleRenditions(
            FileKey(filename = key),
            files = filesToUpload,
            ownerId,
            isPublic,
        )
    }

    private suspend fun createUploadRequest(originalImage: ImmutableImage, size: Int, key: String, suffix: String?, mediaType: String): FileUploadRequest {
        val fileKey = FileKey(filename = key, suffix = suffix, extension = "webp")
        logger.debug { "Creating upload request for $fileKey" }
        val resized = originalImage.resize(size)
        val resizedBytes = resized.bytes(webpWriter)

        return FileUploadRequest.ByteArrayUpload(
            key = fileKey,
            contentType = mediaType,
            data = resizedBytes,
            width = resized.width,
            height = resized.height,
            contentLength = resizedBytes.size.toLong()
        )
    }

    suspend fun ImmutableImage.resize(size: Int): ImmutableImage = withContext(Dispatchers.Default) {
        val ratio = this@resize.height.toDouble() / this@resize.width.toDouble()
        val calculatedHeight = (size * ratio).toInt()

        val resizedImage = if (calculatedHeight < size) {
            this@resize.scaleToHeight(size)
        } else {
            this@resize.scaleToWidth(size)
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

    companion object {
        const val MAX_ASPECT_RATIO = 16.0 / 9.0
    }
}
