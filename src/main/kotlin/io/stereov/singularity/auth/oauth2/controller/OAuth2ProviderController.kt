package io.stereov.singularity.auth.oauth2.controller

import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.oauth2.dto.request.OAuth2ProviderConnectionRequest
import io.stereov.singularity.auth.oauth2.dto.response.OAuth2ProviderConnectionTokenResponse
import io.stereov.singularity.auth.oauth2.service.token.OAuth2ProviderConnectionTokenService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.swagger.v3.oas.annotations.ExternalDocumentation
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users/me/providers/oauth2")
@Tag(name = "OAuth2")
@ConditionalOnProperty("singularity.auth.oauth2.enable", matchIfMissing = false)
class OAuth2ProviderController(
    private val authorizationService: AuthorizationService,
    private val oAuth2ProviderConnectionTokenService: OAuth2ProviderConnectionTokenService,
    private val authProperties: AuthProperties,
    private val cookieCreator: CookieCreator
) {

    @PostMapping("token")
    @Operation(
        summary = "Generate OAuth2ProviderConnectionToken",
        description = """
            Generate an OAuth2ProviderConnectionToken that enables the user to connect new OAuth2 providers.
            
            You can learn more about connecting OAuth2 providers to existing accounts 
            [here](https://singularity.stereov.io/docs/guides/auth/oauth2#connecting-an-oauth2-provider-to-an-existing-account).
            
            ### Tokens
            - A valid [`AccessToken`](https://singularity.stereov.io/docs/guides/auth/tokens#access-token) is required.
            - A valid [`StepUpToken`](https://singularity.stereov.io/docs/guides/auth/tokens#step-up-token)
              is required. This token should match user and session contained in the `AccessToken`.
        """,
        externalDocs = ExternalDocumentation(url = "https://singularity.stereov.io/docs/guides/auth/oauth2#connecting-an-oauth2-provider-to-an-existing-account"),
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(OpenApiConstants.STEP_UP_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.STEP_UP_TOKEN_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Returns the token if header authentication is enabled.",
                content = [Content(schema = Schema(implementation = OAuth2ProviderConnectionTokenResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Invalid or expired `AccessToken` or `StepUpToken`.",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun generateOAuth2ProviderConnectionToken(@RequestBody req: OAuth2ProviderConnectionRequest): ResponseEntity<OAuth2ProviderConnectionTokenResponse> {
        val userId = authorizationService.getUserId()
        val sessionId = authorizationService.getSessionId()

        authorizationService.requireStepUp()
        val token = oAuth2ProviderConnectionTokenService.create(userId, sessionId, req.provider)

        val res = OAuth2ProviderConnectionTokenResponse(
            token = if (authProperties.allowHeaderAuthentication) token.value else null
        )

        return ResponseEntity.ok()
            .header("Set-Cookie", cookieCreator.createCookie(token).toString())
            .body(res)
    }
}
