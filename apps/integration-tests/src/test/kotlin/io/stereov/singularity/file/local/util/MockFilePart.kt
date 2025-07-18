package io.stereov.singularity.file.local.util

import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.file.Path

class MockFilePart(
    private val resource: ClassPathResource
) : FilePart {
    override fun filename(): String = resource.filename!!
    override fun name() = "file"
    override fun headers() = HttpHeaders().apply {
        contentType = MediaType.IMAGE_JPEG
    }

    override fun content(): Flux<DataBuffer?> = DataBufferUtils.read(resource.file.toPath(), DefaultDataBufferFactory(), 4096)
    override fun transferTo(dest: Path): Mono<Void?> = Mono.fromCallable {
        resource.file.copyTo(dest.toFile(), overwrite = true)
        null
    }
}