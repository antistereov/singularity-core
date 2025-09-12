package io.stereov.singularity.auth.core.service.token

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.model.token.SessionToken
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.global.util.Constants
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class SessionTokenService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties
) {

    private val logger = KotlinLogging.logger {}

    suspend fun create(sessionInfo: SessionInfoRequest, issuedAt: Instant = Instant.now()): SessionToken {
        logger.debug { "Creating session token" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
            .claim(Constants.JWT_SESSION_CLAIM, sessionInfo.id)
            .claim(Constants.JWT_BROWSER_CLAIM, sessionInfo.browser)
            .claim(Constants.JWT_OS_CLAIM, sessionInfo.os)
            .build()

        val jwt = jwtService.encodeJwt(claims)

        return SessionToken(sessionInfo.id, sessionInfo.browser, sessionInfo.os, jwt)
    }

    suspend fun extract(tokenValue: String): SessionToken {
        logger.debug { "Extracting session token" }

        val jwt = jwtService.decodeJwt(tokenValue, true)

        val sessionId = jwt.claims[Constants.JWT_SESSION_CLAIM] as? String
            ?: throw InvalidTokenException("No session ID found in claims")
        val browser = jwt.claims[Constants.JWT_BROWSER_CLAIM] as? String
        val os = jwt.claims[Constants.JWT_OS_CLAIM] as? String

        return SessionToken(sessionId, browser, os, jwt)
    }
}