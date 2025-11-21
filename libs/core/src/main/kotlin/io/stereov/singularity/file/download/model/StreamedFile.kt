package io.stereov.singularity.file.download.model

import kotlinx.coroutines.flow.Flow
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType

/**
 * Represents a file streamed as a flow of data buffers.
 *
 * This data class is designed to encapsulate the content of a file being downloaded or streamed
 * in reactive applications. The content is provided as a flow of [DataBuffer], which allows
 * efficient streaming of large files without loading the entire content into memory. Additionally,
 * the content type and the source URL of the file are included for reference.
 *
 * @property content The reactive flow of [DataBuffer] representing the streamed content of the file.
 * @property contentType The media type of the streamed file content (e.g., application/pdf, text/plain).
 * @property url The source URL where the file is being streamed from.
 */
data class StreamedFile(
    val content: Flow<DataBuffer>,
    val contentType: MediaType,
    val url: String
)
