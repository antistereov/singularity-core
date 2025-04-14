package io.stereov.web.global.service.file.util

import org.reactivestreams.Publisher
import org.reactivestreams.Subscriber
import org.springframework.core.io.buffer.DataBuffer
import reactor.core.publisher.Flux
import java.nio.ByteBuffer

class DataBufferPublisher(
    private val flux: Flux<DataBuffer>
) : Publisher<ByteBuffer> {
    override fun subscribe(s: Subscriber<in ByteBuffer>) {
        flux
            .map { buffer ->
                val byteArray = ByteArray(buffer.readableByteCount())
                buffer.read(byteArray)
                ByteBuffer.wrap(byteArray)
            }
            .doOnDiscard(ByteBuffer::class.java) {}
            .subscribe(s)
    }
}
