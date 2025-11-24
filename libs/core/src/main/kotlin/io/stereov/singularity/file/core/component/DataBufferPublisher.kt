package io.stereov.singularity.file.core.component

import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux

/**
 * A utility component that provides methods to process and transform Flux streams of DataBuffer.
 * This class is particularly useful for applications dealing with reactive streams of data buffers,
 * enabling efficient manipulation of data in asynchronous and non-blocking workflows.
 */
@Component
class DataBufferPublisher() {

    /**
     * Converts a reactive stream of [DataBuffer] objects into a single [ByteArray].
     *
     * @param flux a `Flux` stream containing `DataBuffer` objects to be processed.
     * @return a `ByteArray` representing the aggregated data from the given `DataBuffer` stream.
     */
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