package io.stereov.singularity.file.core.model

import org.springframework.http.MediaType

data class DownloadedFile(
    val bytes: ByteArray,
    val contentType: MediaType,
    val url: String,
) {

    val size: Int
        get() = bytes.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DownloadedFile

        if (!bytes.contentEquals(other.bytes)) return false
        if (contentType != other.contentType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + contentType.hashCode()
        return result
    }
}
