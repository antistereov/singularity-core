package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.Token
import io.stereov.singularity.auth.core.model.TokenType
import io.stereov.singularity.global.properties.AppProperties
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class CookieCreator(
    private val appProperties: AppProperties
) {

    private val logger = KotlinLogging.logger {}

    fun createCookie(token: Token<*>, path: String = "/"): ResponseCookie {
        logger.debug { "Creating cookie from token of type ${token.type.cookieName}" }

        val cookie = ResponseCookie.from(token.type.cookieName, token.jwt.tokenValue)
            .httpOnly(true)
            .sameSite("Strict")
            .path(path)

        token.expiresAt
            ?.let { it.epochSecond - Instant.now().epochSecond }
            ?.let { Duration.ofSeconds(it) }
            ?.let { cookie.maxAge(it) }

        if (appProperties.secure) {
            cookie.secure(true)
        }

        return cookie.build()
    }

    suspend fun clearCookie(tokenType: TokenType, path: String = "/"): ResponseCookie {
        logger.debug { "Clearing cookie for ${tokenType.cookieName}" }

        val cookie = ResponseCookie.from(tokenType.cookieName, "")
            .httpOnly(true)
            .sameSite("Strict")
            .maxAge(0)
            .path(path)

        if (appProperties.secure) {
            cookie.secure(true)
        }

        return cookie.build()
    }
}
