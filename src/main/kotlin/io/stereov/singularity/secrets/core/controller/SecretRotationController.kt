package io.stereov.singularity.secrets.core.controller

import io.stereov.singularity.admin.core.dto.RotationStatusResponse
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.secrets.core.service.SecretRotationService
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
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
    @Operation(
        summary = "Trigger Secret Rotation",
        description = """
            Triggers an immediate secret key rotation. This is an asynchronous operation.
            
            You can learn more about key rotation [here](https://singularity.stereov.io/docs/guides/secret-store/basics#secret-rotation).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/secret-store/basics#secret-rotation"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Rotation process successfully triggered.",
                content = [Content(schema = Schema(implementation = SuccessResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired AccessToken.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "AccessToken does not grant [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) access.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun rotateSecretKeys(): ResponseEntity<SuccessResponse> = coroutineScope {
        async { secretRotationService.rotateKeys() }.start()

        return@coroutineScope ResponseEntity.ok(SuccessResponse(true))
    }

    @GetMapping("/rotate-keys/status")
    @Operation(
        summary = "Get Secret Rotation Status",
        description = """
            Returns the status of the current or most recent secret rotation process.
            
            You can learn more about key rotation [here](https://singularity.stereov.io/docs/guides/secret-store/basics#secret-rotation).
            
            **Tokens:**
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
        """,
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),
        ],
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/secret-store/basics#secret-rotation"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The rotation status.",
                content = [Content(schema = Schema(implementation = RotationStatusResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired AccessToken.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            ),
            ApiResponse(
                responseCode = "403",
                description = "AccessToken does not grant [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) access.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun getSecretKeyRotationStatus(): ResponseEntity<RotationStatusResponse> {
        return ResponseEntity.ok(
            this.secretRotationService.getRotationStatus()
        )
    }
}