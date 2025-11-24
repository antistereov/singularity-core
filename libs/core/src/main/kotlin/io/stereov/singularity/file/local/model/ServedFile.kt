package io.stereov.singularity.file.local.model

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import reactor.core.publisher.Flux

/**
 * Represents a file that is served as content, with metadata and streaming data.
 *
 * @property mediaType The media type of the file, indicating the format or type of the content.
 * @property size The size of the file as a string, typically indicating the file size in human-readable format.
 * @property content The reactive stream of data buffers that represents the file content.
 */
data class ServedFile(
    val mediaType: MediaType,
    val size: String,
    val content: Flux<DataBuffer>,
)
