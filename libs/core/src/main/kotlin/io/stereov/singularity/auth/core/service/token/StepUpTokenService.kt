package io.stereov.singularity.auth.core.service.token

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.component.TokenValueExtractor
import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.core.model.token.StepUpToken
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.global.util.Constants
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant
import java.util.*

@Service
class StepUpTokenService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
    private val tokenValueExtractor: TokenValueExtractor,
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = SessionTokenType.StepUp

    suspend fun create(userId: ObjectId, sessionId: UUID, issuedAt: Instant = Instant.now()): StepUpToken {
        logger.debug { "Creating step up token" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
            .subject(userId.toHexString())
            .claim(Constants.JWT_SESSION_CLAIM, sessionId)
            .build()

        val jwt = jwtService.encodeJwt(claims, tokenType.cookieName)

        return StepUpToken(userId, sessionId, jwt)
    }

    suspend fun createForRecovery(userId: ObjectId, sessionId: UUID, exchange: ServerWebExchange, issuedAt: Instant = Instant.now()): StepUpToken {
        logger.debug { "Creating step up token" }

        if (exchange.request.path.toString() != "/api/auth/2fa/totp/recover")
            throw AuthException("Cannot createGroup step up token. This function call is only allowed when it is called from /api/auth/2fa/totp/recover")

        return create(userId, sessionId, issuedAt)
    }

    suspend fun extract(tokenValue: String, currentUserId: ObjectId, currentSessionId: UUID): StepUpToken {
        logger.debug { "Extracting step up token" }

        val jwt = jwtService.decodeJwt(tokenValue, tokenType.cookieName)

        val userId = jwt.subject?.let { ObjectId(it) }
            ?: throw InvalidTokenException("JWT does not contain sub")

        if (userId != currentUserId) {
            throw InvalidTokenException("Step up token is not valid for currently logged in user")
        }

        val sessionId = (jwt.claims[Constants.JWT_SESSION_CLAIM] as? String)
            ?.let { UUID.fromString(it) }
            ?: throw InvalidTokenException("JWT does not contain session id")

        if (sessionId != currentSessionId) {
            throw InvalidTokenException("Step up token is not valid for current session")
        }

        return StepUpToken(userId, sessionId, jwt)
    }

    suspend fun extract(exchange: ServerWebExchange, currentUserId: ObjectId, currentSessionId: UUID): StepUpToken {
        val tokenValue = tokenValueExtractor.extractValue(exchange, tokenType)

        return extract(tokenValue, currentUserId, currentSessionId)
    }
}
