package io.stereov.singularity.file.core.model

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import io.stereov.singularity.file.core.exception.FileException
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import reactor.core.publisher.Flux

/**
 * Represents a file served as content, with metadata and streaming data.
 *
 * @property size The size of the file as a string, typically indicating the file size in human-readable format.
 * @property content The reactive stream of data buffers that represents the file content.
 */
data class ServedFile(
    val size: String,
    val content: Flux<out DataBuffer>,
    val rendition: FileRendition,
    val isPublic: Boolean
) {

    fun parseMediaType(): Result<MediaType, FileException.Metadata> {
        return runCatching { MediaType.parseMediaType(rendition.contentType) }
            .mapError { ex ->
                FileException.Metadata(
                    "Invalid media type ${rendition.contentType} saved in metadata for file with key ${rendition.key}: ${ex.message}",
                    ex
                )
            }
    }
}