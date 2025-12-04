package io.stereov.singularity.auth.oauth2.controller

import com.github.michaelbull.result.getOrThrow
import io.stereov.singularity.auth.core.dto.response.IdentityProviderResponse
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.oauth2.dto.request.AddPasswordAuthenticationRequest
import io.stereov.singularity.auth.oauth2.exception.DisconnectProviderException
import io.stereov.singularity.auth.oauth2.exception.SetPasswordException
import io.stereov.singularity.auth.oauth2.service.IdentityProviderService
import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
import io.stereov.singularity.auth.token.exception.StepUpTokenExtractionException
import io.stereov.singularity.global.annotation.ThrowsDomainError
import io.stereov.singularity.global.model.OpenApiConstants
import io.stereov.singularity.principal.core.dto.response.UserResponse
import io.stereov.singularity.principal.core.exception.FindUserByIdException
import io.stereov.singularity.principal.core.exception.PrincipalMapperException
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.model.identity.UserIdentity
import io.stereov.singularity.principal.core.service.UserService
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ServerWebExchange
import java.util.*

@RestController
@RequestMapping("/api/users")
@ConditionalOnProperty("singularity.auth.oauth2.enable", matchIfMissing = false)
@Tag(name = "OAuth2", description = "Operations related to connecting and disconnecting identity providers to existing accounts.")
class IdentityProviderController(
    private val identityProviderService: IdentityProviderService,
    private val authorizationService: AuthorizationService,
    private val principalMapper: PrincipalMapper,
    private val userService: UserService,
) {

    @GetMapping("me/providers")
    @Operation(
        summary = "Get Identity Providers",
        description = """
            Get a list of connected identity providers for the current user.
            
            Users can connect multiple OAuth2 providers to their account.
            You can learn more about this [here](https://singularity.stereov.io/docs/guides/auth/oauth2).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/oauth2#getting-connected-providers"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "The list of identity providers.",
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        FindUserByIdException::class,
    ])
    suspend fun getIdentityProviders(): ResponseEntity<List<IdentityProviderResponse>> {
        val userId = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }
            .principalId

        val user = userService.findById(userId)
            .getOrThrow { FindUserByIdException.from(it) }

        val identityProviders = user.sensitive.identities.providers
            .map { IdentityProviderResponse(it.key) }
            .toMutableList()
            .apply {
                if (user.sensitive.identities.password != null) {
                    add(IdentityProviderResponse(UserIdentity.PASSWORD_IDENTITY))
                }
            }

        return ResponseEntity.ok(identityProviders)
    }

    @PostMapping("me/providers/password")
    @Operation(
        summary = "Add Password Authentication",
        description = """
            Create a new identity provider for the current user that allows the user to login with a password.
            
            You can learn more about this [here](https://singularity.stereov.io/docs/guides/auth/oauth2#adding-password-authentication).
            
            ### Requirements
            - The user registered via an OAuth2 provider.
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - A valid [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              is required. This token should match user and session contained in the `AccessToken`.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/oauth2#adding-password-authentication"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.STEP_UP_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Success.",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            )
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        StepUpTokenExtractionException::class,
        FindUserByIdException::class,
        SetPasswordException::class,
        PrincipalMapperException::class
    ])
    suspend fun addPasswordAuthentication(
        @RequestBody @Valid req: AddPasswordAuthenticationRequest,
        exchange: ServerWebExchange
    ): ResponseEntity<UserResponse> {
        val authentication = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

        authorizationService.requireStepUp(authentication, exchange)
            .getOrThrow { when (it) { is StepUpTokenExtractionException -> it } }

        var user = userService.findById(authentication.principalId)
            .getOrThrow { FindUserByIdException.from(it) }

        user = identityProviderService.setPassword(req, user)
            .getOrThrow { when (it) { is SetPasswordException -> it } }

        val response = principalMapper.toResponse(user)
            .getOrThrow { when (it) { is PrincipalMapperException -> it } }

        return ResponseEntity.ok(response)
    }

    @DeleteMapping("me/providers/{provider}")
    @Operation(
        summary = "Delete Identity Provider",
        description = """
            Delete an identity provider from the connected identity providers of the current user.
            
            ### Requirements
            - You are not allowed to delete the password identity or the only existing identity.
            
            A [security alert](https://singularity.stereov.io/docs/guides/auth/security-alerts#oauth2-specific-alerts)
            will be sent to the user's email if this setting is enabled and
            email is [enabled and configured correctly](https://singularity.stereov.io/docs/guides/email/configuration).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - A valid [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              is required. This token should match user and session contained in the `AccessToken`.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/oauth2#disconnecting-an-oauth2-provider"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.STEP_UP_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Success.",
                content = [Content(schema = Schema(implementation = UserResponse::class))]
            ),
        ]
    )
    @ThrowsDomainError([
        AccessTokenExtractionException::class,
        AuthenticationException.AuthenticationRequired::class,
        StepUpTokenExtractionException::class,
        FindUserByIdException::class,
        DisconnectProviderException::class,
        PrincipalMapperException::class
    ])
    suspend fun deleteIdentityProvider(
        @PathVariable provider: String,
        @RequestParam locale: Locale?,
        exchange: ServerWebExchange
    ): ResponseEntity<UserResponse> {
        val authentication = authorizationService.getAuthenticationOutcome()
            .getOrThrow { when (it) { is AccessTokenExtractionException -> it } }
            .requireAuthentication()
            .getOrThrow { when (it) { is AuthenticationException.AuthenticationRequired -> it } }

        authorizationService.requireStepUp(authentication, exchange)
            .getOrThrow { when (it) { is StepUpTokenExtractionException -> it } }

        var user = userService.findById(authentication.principalId)
            .getOrThrow { FindUserByIdException.from(it) }

        user = identityProviderService.disconnect(provider, user, locale)
            .getOrThrow { when (it) { is DisconnectProviderException -> it } }

        val response = principalMapper.toResponse(user)
            .getOrThrow { when (it) { is PrincipalMapperException -> it } }

        return ResponseEntity.ok(response)
    }
}
