package io.stereov.singularity.file.core.model

import org.springframework.http.codec.multipart.FilePart

sealed interface FileUploadRequest {
    val key: FileKey
    val contentType: String
    val contentLength: Long
    val data: Any
    val width: Int?
    val height: Int?

    data class FilePartUpload(
        override val key: FileKey,
        override val contentType: String,
        override val contentLength: Long,
        override val data: FilePart,
        override val width: Int? = null,
        override val height: Int? = null
    ) : FileUploadRequest

    data class ByteArrayUpload(
        override val key: FileKey,
        override val contentType: String,
        override val contentLength: Long,
        override val data: kotlin.ByteArray,
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
