package io.stereov.singularity.file.core.component

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong

@Component
class DataBufferPublisher() {

    fun toFlux(flux: Flux<DataBuffer>, contentLength: AtomicLong = AtomicLong()): Flux<ByteBuffer> {
        return flux
            .flatMapIterable { buffer ->
                contentLength.addAndGet(buffer.readableByteCount().toLong())
                buffer.readableByteBuffers().asSequence().toList()
            }
    }

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