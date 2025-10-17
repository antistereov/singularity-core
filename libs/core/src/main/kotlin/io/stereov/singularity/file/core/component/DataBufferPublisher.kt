package io.stereov.singularity.file.core.component

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

@Component
class DataBufferPublisher() {

    suspend fun toSingleByteArray(flux: Flux<DataBuffer>): ByteArray {

        return flux
            .collectList()
            .map { buffers ->
                val totalSize = buffers.sumOf { it.readableByteCount() }
                val result = ByteArray(totalSize)
                var offset = 0
                buffers.forEach { buffer ->
                    val count = buffer.readableByteCount()
                    buffer.read(result, offset, count)
                    offset += count
                }
                result
            }.awaitSingle()
    }
}