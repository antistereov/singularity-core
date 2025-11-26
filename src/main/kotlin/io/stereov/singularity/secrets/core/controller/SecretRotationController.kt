package io.stereov.singularity.secrets.core.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.global.model.SuccessResponse
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.secrets.core.dto.RotationStatusResponse
import io.stereov.singularity.secrets.core.exception.SecretRotationException
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
    private val secretRotationService: SecretRotationService,
    private val authorizationService: AuthorizationService
) {

    @PostMapping("/rotate-keys")
    @Operation(
        summary = "Trigger Secret Rotation",
        description = """
            Triggers an immediate secret key rotation. This is an asynchronous operation.
            
            You can learn more about key rotation [here](https://singularity.stereov.io/docs/guides/secret-store/basics#secret-key-rotation).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/secret-store/basics#secret-key-rotation"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Rotation process successfully triggered.",
                content = [Content(schema = Schema(implementation = SuccessResponse::class))]
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        AuthenticationException.RoleRequired::class,
        SecretRotationException.Ongoing::class
    ])
    suspend fun rotateSecretKeys(): ResponseEntity<SuccessResponse> = coroutineScope {
        authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .requireRole(Role.User.ADMIN)
            .getOrThrow { when (it) { is AuthenticationException.RoleRequired -> it } }

        if (secretRotationService.rotationOngoing()) {
            throw SecretRotationException.Ongoing("Rotation is already ongoing")
        }

        async { secretRotationService.rotateKeys() }.start()

        return@coroutineScope ResponseEntity.ok(SuccessResponse(true))
    }

    @GetMapping("/rotate-keys/status")
    @Operation(
        summary = "Get Secret Rotation Status",
        description = """
            Returns the status of the current or most recent secret rotation process.
            
            You can learn more about key rotation [here](https://singularity.stereov.io/docs/guides/secret-store/basics#secret-key-rotation).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token)
              with [`ADMIN`](https://singularity.stereov.io/docs/guides/auth/roles#admins) permissions is required.
        """,
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER, scopes = [OpenApiConstants.ADMIN_SCOPE]),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE, scopes = [OpenApiConstants.ADMIN_SCOPE]),
        ],
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/secret-store/basics#secret-key-rotation"),
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The rotation status.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        AuthenticationException.RoleRequired::class,
    ])
    suspend fun getSecretKeyRotationStatus(): ResponseEntity<RotationStatusResponse> {
        authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .requireRole(Role.User.ADMIN)
            .getOrThrow { when (it) { is AuthenticationException.RoleRequired -> it } }

        return ResponseEntity.ok(
            secretRotationService.getRotationStatus()
        )
    }
}
