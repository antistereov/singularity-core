package io.stereov.singularity.file.local.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.file.core.exception.FileException
import io.stereov.singularity.file.local.service.LocalFileStorage
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux

@Controller
@RequestMapping("/api/assets")
@ConditionalOnProperty(prefix = "singularity.file.storage", value = ["type"], havingValue = "local", matchIfMissing = true)
class LocalFileStorageController(
    private val localFileStorage: LocalFileStorage,
    private val authorizationService: AuthorizationService,
) {

    @GetMapping("/**")
    @Operation(
        summary = "Get File from Local File Storage",
        description = """
            Retrieve a file from the [local file storage](https://singularity.stereov.io/docs/guides/file-storage/local).
            
            This endpoint will be provided if *Singularity* is configured to use the local file storage implementation
            of file storage. You can learn more [here](http://localhost:3000/docs/guides/file-storage/introduction#configuration).
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/file-storage/local"),
    )
    @ThrowsDomainError(errorClasses = [
        AccessTokenExtractionException::class,
        FileException::class,
    ])
    suspend fun serveFile(
        exchange: ServerWebExchange
    ): ResponseEntity<Flux<DataBuffer>> {
        val fullRequestPath = exchange.request.uri.path
        val basePath = "/api/assets/"
        val key = fullRequestPath.removePrefix(basePath)

        val authenticationOutcome = authorizationService.getAuthenticationOutcome()
            .getOrThrow { ex -> when (ex) {
                is AccessTokenExtractionException -> ex
            } }

        val file = localFileStorage.serveFile(authenticationOutcome, key)
            .getOrThrow { ex -> when (ex) {
                is FileException -> ex
            } }

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_LENGTH, file.size)
            .contentType(file.mediaType)
            .body(file.content)
    }
}
