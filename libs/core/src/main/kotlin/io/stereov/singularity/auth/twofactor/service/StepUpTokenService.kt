package io.stereov.singularity.auth.twofactor.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.core.exception.model.TwoFactorAuthDisabledException
import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.auth.core.service.TokenValueExtractor
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.twofactor.model.StepUpToken
import io.stereov.singularity.auth.twofactor.model.TwoFactorTokenType
import io.stereov.singularity.global.util.Constants
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant

@Service
class StepUpTokenService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
    private val authenticationService: AuthenticationService,
    private val twoFactorAuthService: TwoFactorAuthService,
    private val tokenValueExtractor: TokenValueExtractor,
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = TwoFactorTokenType.StepUp

    suspend fun create(code: Int, issuedAt: Instant = Instant.now()): StepUpToken {
        logger.debug { "Creating step up token" }


        val user = authenticationService.getCurrentUser()
        val deviceId = authenticationService.getCurrentDeviceId()

        twoFactorAuthService.validateTwoFactorCode(user, code)

        return create(user.id, deviceId, issuedAt)
    }

    suspend fun create(userId: ObjectId, deviceId: String, issuedAt: Instant = Instant.now()): StepUpToken {
        logger.debug { "Creating step up token" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
            .subject(userId.toHexString())
            .claim(Constants.JWT_DEVICE_CLAIM, deviceId)
            .build()

        val jwt = jwtService.encodeJwt(claims)

        return StepUpToken(userId, deviceId, jwt)
    }

    suspend fun createForRecovery(userId: ObjectId, deviceId: String, exchange: ServerWebExchange, issuedAt: Instant = Instant.now()): StepUpToken {
        logger.debug { "Creating step up token" }

        if (exchange.request.path.toString() != "/api/user/2fa/recovery")
            throw AuthException("Cannot create step up token. This function call is only allowed when it is called from /auth/2fa/recovery")

        return create(userId, deviceId, issuedAt)
    }

    suspend fun extract(exchange: ServerWebExchange): StepUpToken {
        logger.debug { "Extracting step up token" }

        val token = tokenValueExtractor.extractValue(exchange, tokenType)

        val jwt = jwtService.decodeJwt(token, true)

        val userId = jwt.subject?.let { ObjectId(it) }
            ?: throw InvalidTokenException("JWT does not contain sub")

        if (userId != authenticationService.getCurrentUserId()) {
            throw InvalidTokenException("Step up token is not valid for currently logged in user")
        }

        if (!authenticationService.getCurrentUser().sensitive.security.twoFactor.enabled) {
            throw TwoFactorAuthDisabledException()
        }

        val deviceId = jwt.claims[Constants.JWT_DEVICE_CLAIM] as? String
            ?: throw InvalidTokenException("JWT does not contain device id")

        if (deviceId != authenticationService.getCurrentDeviceId()) {
            throw InvalidTokenException("Step up token is not valid for current device")
        }

        return StepUpToken(userId, deviceId, jwt)
    }
}
