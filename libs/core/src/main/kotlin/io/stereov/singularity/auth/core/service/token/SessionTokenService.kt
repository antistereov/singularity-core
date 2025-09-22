package io.stereov.singularity.auth.core.service.token

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.component.TokenValueExtractor
import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.auth.core.model.token.SessionToken
import io.stereov.singularity.auth.core.model.token.SessionTokenType
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.global.util.Constants
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import org.springframework.web.server.ServerWebExchange
import java.time.Instant

@Service
class SessionTokenService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
    private val tokenValueExtractor: TokenValueExtractor
) {

    private val logger = KotlinLogging.logger {}
    val tokenType = "session"

    suspend fun create(sessionInfo: SessionInfoRequest? = null, issuedAt: Instant = Instant.now()): SessionToken {
        logger.debug { "Creating session token" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))

        if (sessionInfo?.browser != null) {
            claims.claim(Constants.JWT_BROWSER_CLAIM, sessionInfo.browser)
        }
        if (sessionInfo?.os != null) {
            claims.claim(Constants.JWT_OS_CLAIM, sessionInfo.os)
        }

        val jwt = jwtService.encodeJwt(claims.build(), tokenType)

        return SessionToken(sessionInfo?.browser, sessionInfo?.os, jwt)
    }

    suspend fun extract(exchange: ServerWebExchange): SessionToken {
        val tokenValue = tokenValueExtractor.extractValue(exchange, SessionTokenType.Session)

        return extract(tokenValue)

    }

    suspend fun extract(tokenValue: String): SessionToken {
        logger.debug { "Extracting session token" }

        val jwt = jwtService.decodeJwt(tokenValue, tokenType)
        val browser = jwt.claims[Constants.JWT_BROWSER_CLAIM] as? String
        val os = jwt.claims[Constants.JWT_OS_CLAIM] as? String

        return SessionToken(browser, os, jwt)
    }
}