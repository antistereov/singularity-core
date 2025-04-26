package io.stereov.singularity.global.service.file.util

import org.springframework.core.io.buffer.DataBuffer
import reactor.core.publisher.Flux
import java.nio.ByteBuffer

class DataBufferPublisher(
    private val flux: Flux<DataBuffer>
)  {
    fun toFlux(): Flux<ByteBuffer> {
        return flux.map {
            buffer ->
                val byteArray = ByteArray(buffer.readableByteCount())
                buffer.read(byteArray)
                ByteBuffer.wrap(byteArray)
        }
    }
}
