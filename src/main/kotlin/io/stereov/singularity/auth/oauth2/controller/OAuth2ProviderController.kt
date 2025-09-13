package io.stereov.singularity.auth.oauth2.controller

import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.oauth2.dto.request.OAuth2ProviderConnectionRequest
import io.stereov.singularity.auth.oauth2.dto.response.OAuth2ProviderConnectionTokenResponse
import io.stereov.singularity.auth.oauth2.service.token.OAuth2ProviderConnectionTokenService
import io.stereov.singularity.global.model.ErrorResponse
import io.stereov.singularity.global.model.OpenApiConstants
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth/providers/oauth2")
@Tag(name = "OAuth2 Identity Provider", description = "Operations related to OAuth2 identity providers")
class OAuth2ProviderController(
    private val authorizationService: AuthorizationService,
    private val oAuth2ProviderConnectionTokenService: OAuth2ProviderConnectionTokenService,
    private val authProperties: AuthProperties,
    private val cookieCreator: CookieCreator
) {

    @GetMapping("oauth2/token")
    @Operation(
        summary = "Generate an OAuth2ProviderConnectionToken",
        description = "Generate an OAuth2ProviderConnectionToken that enables the user to connect new OAuth2 providers.",
        security = [
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_HEADER),
            SecurityRequirement(OpenApiConstants.ACCESS_TOKEN_COOKIE),
            SecurityRequirement(OpenApiConstants.STEP_UP_HEADER),
            SecurityRequirement(OpenApiConstants.STEP_UP_COOKIE)
        ],
        responses = [
            ApiResponse(
                responseCode = "200",
                description = "Success.",
                content = [Content(schema = Schema(implementation = OAuth2ProviderConnectionTokenResponse::class))]
            ),
            ApiResponse(
                responseCode = "401",
                description = "Unauthorized",
                content = [Content(schema = Schema(implementation = ErrorResponse::class))]
            )
        ]
    )
    suspend fun generateOAuth2ProviderConnectionToken(@RequestBody req: OAuth2ProviderConnectionRequest): ResponseEntity<OAuth2ProviderConnectionTokenResponse> {
        val userId = authorizationService.getCurrentUserId()
        val token = oAuth2ProviderConnectionTokenService.create(userId, req.session.id, req.provider)

        val res = OAuth2ProviderConnectionTokenResponse(
            token = if (authProperties.allowHeaderAuthentication) token.value else null
        )

        return ResponseEntity.ok()
            .header("Set-Cookie", cookieCreator.createCookie(token).toString())
            .body(res)
    }
}
