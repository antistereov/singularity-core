package io.stereov.singularity.auth.twofactor.controller

import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.dto.response.LoginResponse
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.token.AccessTokenService
import io.stereov.singularity.auth.core.service.token.RefreshTokenService
import io.stereov.singularity.auth.core.service.token.StepUpTokenService
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.twofactor.dto.request.TwoFactorAuthenticationRequest
import io.stereov.singularity.auth.twofactor.dto.response.StepUpResponse
import io.stereov.singularity.auth.twofactor.model.token.TwoFactorTokenType
import io.stereov.singularity.auth.twofactor.service.TwoFactorAuthenticationService
import io.stereov.singularity.user.core.mapper.UserMapper
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import java.util.*

@RestController
@RequestMapping("/api/auth/2fa")
@Tag(
    name = "Two Factor Authentication",
    description = "Operations related to two-factor authentication"
)
class TwoFactorAuthenticationController(
    private val twoFactorAuthService: TwoFactorAuthenticationService,
    private val authProperties: AuthProperties,
    private val geolocationService: GeolocationService,
    private val userMapper: UserMapper,
    private val accessTokenService: AccessTokenService,
    private val refreshTokenService: RefreshTokenService,
    private val stepUpTokenService: StepUpTokenService,
    private val cookieCreator: CookieCreator,
    private val authorizationService: AuthorizationService,
) {

    @PostMapping("/login")
    suspend fun verifyLogin(
        exchange: ServerWebExchange,
        @RequestBody req: TwoFactorAuthenticationRequest
    ): ResponseEntity<LoginResponse> {
        val user = twoFactorAuthService.validateTwoFactor(exchange, req)
        val sessionId = UUID.randomUUID()

        val accessToken = accessTokenService.create(user, sessionId)
        val refreshToken = refreshTokenService.create(user.id, sessionId, req.session, exchange)

        val clearTwoFactorCookie = cookieCreator.clearCookie(TwoFactorTokenType.Authentication)

        val res = LoginResponse(
            twoFactorRequired = false,
            user = userMapper.toResponse(user),
            accessToken = if (authProperties.allowHeaderAuthentication) accessToken.value else null,
            refreshToken = if (authProperties.allowHeaderAuthentication) refreshToken.value else null,
            allowedTwoFactorMethods = null,
            twoFactorAuthenticationToken = null,
            location = geolocationService.getLocationOrNull(exchange.request)
        )

        return ResponseEntity.ok()
            .header("Set-Cookie", clearTwoFactorCookie.toString())
            .header("Set-Cookie", cookieCreator.createCookie(accessToken).toString())
            .header("Set-Cookie", cookieCreator.createCookie(refreshToken).toString())
            .body(res)
    }

    @PostMapping("/step-up")
    suspend fun verifyStepUp(@RequestBody req: TwoFactorAuthenticationRequest, exchange: ServerWebExchange): ResponseEntity<StepUpResponse> {
        val user = twoFactorAuthService.validateTwoFactor(exchange, req)
        val sessionId = authorizationService.getCurrentSessionId()

        val stepUpTokenCookie = stepUpTokenService.create(user.id, sessionId)

        return ResponseEntity.ok()
            .header("Set-Cookie", cookieCreator.createCookie(stepUpTokenCookie).toString())
            .body(StepUpResponse(if (authProperties.allowHeaderAuthentication) stepUpTokenCookie.value else null))
    }
}
