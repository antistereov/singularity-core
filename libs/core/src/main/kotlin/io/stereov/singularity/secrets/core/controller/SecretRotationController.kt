package io.stereov.singularity.secrets.core.controller

import io.stereov.singularity.admin.core.dto.RotationStatusResponse
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.secrets.core.service.SecretRotationService
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/security/secrets")
@Tag(name = "Security", description = "Operations related to managing the servers security.")
class SecretRotationController(
    private val secretRotationService: SecretRotationService
) {

    @PostMapping("/rotate-keys")
    suspend fun rotateKeys(): ResponseEntity<SuccessResponse> = coroutineScope {
        async { secretRotationService.rotateKeys() }.start()

        return@coroutineScope ResponseEntity.ok(SuccessResponse(true))
    }

    @GetMapping("/rotate-keys/status")
    suspend fun rotationOngoing(): ResponseEntity<RotationStatusResponse> {
        return ResponseEntity.ok(
            this.secretRotationService.getRotationStatus()
        )
    }
}