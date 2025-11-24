package io.stereov.singularity.file.local.model

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import reactor.core.publisher.Flux

data class ServedFile(
    val mediaType: MediaType,
    val size: String,
    val content: Flux<DataBuffer>,
)
