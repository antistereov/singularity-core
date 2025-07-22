package io.stereov.singularity.file.local.controller

import io.stereov.singularity.content.common.content.model.ContentAccessRole
import io.stereov.singularity.file.core.service.FileMetadataService
import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import java.nio.file.Files
import java.nio.file.Paths

@Controller
@RequestMapping("/api/assets/private")
@ConditionalOnProperty(prefix = "singularity.file.storage", value = ["type"], havingValue = "local", matchIfMissing = true)
class LocalFileStorageController(
    private val metadataService: FileMetadataService,
    private val properties: LocalFileStorageProperties,
) {

    @GetMapping("/**")
    suspend fun servePrivateFile(
        exchange: ServerWebExchange
    ): ResponseEntity<Flux<DataBuffer>> {
        val fullRequestPath = exchange.request.uri.path
        val basePath = "/api/assets/private"
        val key = fullRequestPath.removePrefix(basePath)

        if (key.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing file key")
        }

        val metadata = metadataService.findByKey(key)
        metadataService.requireAuthorization(metadata, ContentAccessRole.VIEWER)

        val filePath = Paths.get(properties.privatePath).resolve(key).normalize()

        if (!filePath.startsWith(basePath)) {
            throw ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid path access")
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_LENGTH, Files.size(filePath).toString())
            .contentType(metadata.contentType)
            .body(DataBufferUtils.read(filePath, DefaultDataBufferFactory(), 4096))
    }
}
