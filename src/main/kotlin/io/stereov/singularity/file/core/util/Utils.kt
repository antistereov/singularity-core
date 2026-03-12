package io.stereov.singularity.file.core.util

import com.github.michaelbull.result.*
import io.stereov.singularity.file.core.exception.FileException
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart

fun FilePart.mediaType(): Result<MediaType, FileException.UnsupportedMediaType> {
    return runCatching {
        this.headers().contentType
    }.mapError { ex ->
        FileException.UnsupportedMediaType("Failed to parse media type: ${ex.message}", ex)
    }.andThen { it.toResultOr {
        FileException.UnsupportedMediaType("No content type is specified")
    } }
}