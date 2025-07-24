package io.stereov.singularity.file.local.controller

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.model.AccessType
import io.stereov.singularity.content.common.content.model.ContentAccessRole
import io.stereov.singularity.file.core.exception.model.FileNotFoundException
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
import kotlin.io.path.absolute
import kotlin.io.path.exists

@Controller
@RequestMapping("/api/assets")
@ConditionalOnProperty(prefix = "singularity.file.storage", value = ["type"], havingValue = "local", matchIfMissing = true)
class LocalFileStorageController(
    private val metadataService: FileMetadataService,
    private val properties: LocalFileStorageProperties,
) {

    private val logger = KotlinLogging.logger {}

    @GetMapping("/**")
    suspend fun serveFile(
        exchange: ServerWebExchange
    ): ResponseEntity<Flux<DataBuffer>> {
        val fullRequestPath = exchange.request.uri.path
        val basePath = "/api/assets/"
        val key = fullRequestPath.removePrefix(basePath)

        logger.debug { "Accessing asset $key" }

        if (key.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing file key")
        }

        val baseFileDir = Paths.get(properties.fileDirectory).toAbsolutePath().normalize()
        val filePath = Paths.get(properties.fileDirectory).resolve(key).normalize().absolute()

        val metadata = metadataService.findByKeyOrNull(key)

        if (metadata == null) {
            filePath.toFile().delete()
            throw FileNotFoundException(filePath.toFile())
        }

        if (!filePath.exists()) {
            metadataService.deleteByKey(key)
            throw FileNotFoundException(filePath.toFile())
        }

        if (!filePath.startsWith(baseFileDir)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid path access: trying to access file $filePath outside of specified directory ${properties.fileDirectory}")
        }


        if (metadata.access.visibility != AccessType.PUBLIC) {
            metadataService.requireAuthorization(metadata, ContentAccessRole.VIEWER)
        }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_LENGTH, Files.size(filePath).toString())
            .contentType(metadata.contentType)
            .body(DataBufferUtils.read(filePath, DefaultDataBufferFactory(), 4096))
    }
}
