package io.stereov.singularity.file.util

import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class MockFilePart(
    private val resource: File
) : FilePart {
    override fun filename(): String = resource.name
    override fun name() = "file"
    override fun headers() = HttpHeaders().apply {
        contentType = MediaType.IMAGE_JPEG
        contentLength = Files.size(resource.toPath())
    }

    override fun content(): Flux<DataBuffer?> = DataBufferUtils.read(resource.toPath(), DefaultDataBufferFactory(), 4096)
    override fun transferTo(dest: Path): Mono<Void?> = Mono.fromCallable {
        resource.copyTo(dest.toFile(), overwrite = true)
        null
    }
}
