package io.stereov.singularity.auth.core.component

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.token.SecurityToken
import io.stereov.singularity.auth.core.model.token.SecurityTokenType
import io.stereov.singularity.global.properties.AppProperties
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

@Component
class CookieCreator(
    private val appProperties: AppProperties
) {

    private val logger = KotlinLogging.logger {}

    fun createCookie(token: SecurityToken<*>, path: String = "/", sameSite: String = "Strict"): ResponseCookie {
        logger.debug { "Creating cookie from token of type ${token.type.cookieName}" }

        val cookie = ResponseCookie.from(token.type.cookieName, token.jwt.tokenValue)
            .httpOnly(true)
            .sameSite(sameSite)
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

    suspend fun clearCookie(securityTokenType: SecurityTokenType, path: String = "/"): ResponseCookie {
        logger.debug { "Clearing cookie for ${securityTokenType.cookieName}" }

        val cookie = ResponseCookie.from(securityTokenType.cookieName, "")
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
