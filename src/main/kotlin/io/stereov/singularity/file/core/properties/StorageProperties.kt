package io.stereov.singularity.file.core.properties

import io.stereov.singularity.file.core.exception.model.FileTooLargeException
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.web.server.ServerWebExchange

@ConfigurationProperties(prefix = "singularity.file.storage")
data class StorageProperties(
    val type: StorageType = StorageType.LOCAL,
    val maxFileSize: Long = 5 * 1024 * 1024L
) {

    fun validateContentLength(exchange: ServerWebExchange) {
        val contentLength = exchange.request.headers.contentLength

       validateContentLength(contentLength)
    }

    fun validateContentLength(contentLength: Long) {
        if (contentLength < 0) {
            throw IllegalArgumentException("Missing Content-Length header")
        }

        if (contentLength > maxFileSize) {
            throw FileTooLargeException("Filesize exceeds limit of ${maxFileSize / 1024 / 1024}MB")
        }
    }
}
