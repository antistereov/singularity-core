package io.stereov.singularity.auth.twofactor.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.core.exception.model.TwoFactorAuthDisabledException
import io.stereov.singularity.auth.core.service.AuthorizationService
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
    private val authorizationService: AuthorizationService,
    private val twoFactorService: TwoFactorService,
    private val tokenValueExtractor: TokenValueExtractor,
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = TwoFactorTokenType.StepUp

    suspend fun create(code: Int, issuedAt: Instant = Instant.now()): StepUpToken {
        logger.debug { "Creating step up token" }


        val user = authorizationService.getCurrentUser()
        val sessionId = authorizationService.getCurrentsessionId()

        twoFactorService.validateTwoFactorCode(user, code)

        return create(user.id, sessionId, issuedAt)
    }

    suspend fun create(userId: ObjectId, sessionId: String, issuedAt: Instant = Instant.now()): StepUpToken {
        logger.debug { "Creating step up token" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
            .subject(userId.toHexString())
            .claim(Constants.JWT_session_CLAIM, sessionId)
            .build()

        val jwt = jwtService.encodeJwt(claims)

        return StepUpToken(userId, sessionId, jwt)
    }

    suspend fun createForRecovery(userId: ObjectId, sessionId: String, exchange: ServerWebExchange, issuedAt: Instant = Instant.now()): StepUpToken {
        logger.debug { "Creating step up token" }

        if (exchange.request.path.toString() != "/api/auth/2fa/recover")
            throw AuthException("Cannot create step up token. This function call is only allowed when it is called from /api/auth/2fa/recover")

        return create(userId, sessionId, issuedAt)
    }

    suspend fun extract(exchange: ServerWebExchange): StepUpToken {
        logger.debug { "Extracting step up token" }

        if (!authorizationService.getCurrentUser().sensitive.security.twoFactor.enabled) {
            throw TwoFactorAuthDisabledException()
        }

        val token = tokenValueExtractor.extractValue(exchange, tokenType)

        val jwt = jwtService.decodeJwt(token, true)

        val userId = jwt.subject?.let { ObjectId(it) }
            ?: throw InvalidTokenException("JWT does not contain sub")

        if (userId != authorizationService.getCurrentUserId()) {
            throw InvalidTokenException("Step up token is not valid for currently logged in user")
        }

        val sessionId = jwt.claims[Constants.JWT_session_CLAIM] as? String
            ?: throw InvalidTokenException("JWT does not contain session id")

        if (sessionId != authorizationService.getCurrentsessionId()) {
            throw InvalidTokenException("Step up token is not valid for current session")
        }

        return StepUpToken(userId, sessionId, jwt)
    }
}
