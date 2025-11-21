package io.stereov.singularity.file.core.model

import org.springframework.http.codec.multipart.FilePart

/**
 * Represents a request for uploading a file.
 * This sealed interface is implemented by specific
 * types of file upload requests, such as [FilePartUpload] and [ByteArrayUpload].
 *
 * Each upload request includes details like a unique file key, content type, raw data, and optional
 * dimensions such as width and height. The concrete implementations of this interface
 * define the structure of the upload data.
 *
 * @property key A unique identifier for the file being uploaded.
 * @property contentType The MIME type of the file (e.g., image/png, application/pdf).
 * @property data The file's raw data, which varies depending on the specific implementation.
 * @property width The optional width of the file, applicable for media files like images or videos.
 * @property height The optional height of the file, applicable for media files like images or videos.
 */
sealed interface FileUploadRequest {
    val key: FileKey
    val contentType: String
    val data: Any
    val width: Int?
    val height: Int?

    /**
     * Represents a file upload request with specific metadata and file part details.
     *
     * This class is used to facilitate the upload of file parts, attaching information such as the file's
     * unique key, MIME type, and optional dimensions. The uploaded file part can represent a specific portion
     * or the entire content of a file.
     *
     * @property key The unique identifier for the file, represented by a [FileKey].
     * @property contentType The MIME type of the file (e.g., image/jpeg, application/pdf).
     * @property data The file part content being uploaded.
     * @property width The width of the file in pixels, if applicable (e.g., for images or videos).
     * @property height The height of the file in pixels, if applicable (e.g., for images or videos).
     */
    data class FilePartUpload(
        override val key: FileKey,
        override val contentType: String,
        override val data: FilePart,
        override val width: Int? = null,
        override val height: Int? = null
    ) : FileUploadRequest

    /**
     * Represents a file upload request using a raw byte array as the file data.
     *
     * This class is designed to upload file data stored in-memory in the form of a [ByteArray].
     * It extends the [FileUploadRequest] interface, providing functionality for specifying the
     * necessary metadata such as file key, content type, and optional dimensions for files
     * that have width and height attributes (e.g., images or videos).
     *
     * @property key A unique identifier for the file, represented by a [FileKey].
     * @property contentType The MIME type of the file (e.g., image/png, application/pdf).
     * @property data The raw binary content of the file in the form of a `ByteArray`.
     * @property width Optional width of the file, if applicable (e.g., width of an image in pixels).
     * @property height Optional height of the file, if applicable (e.g., height of an image in pixels).
     */
    data class ByteArrayUpload(
        override val key: FileKey,
        override val contentType: String,
        override val data: ByteArray,
        override val width: Int? = null,
        override val height: Int? = null
    ) : FileUploadRequest {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as ByteArrayUpload

            if (key != other.key) return false
            if (contentType != other.contentType) return false
            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + contentType.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }
}
