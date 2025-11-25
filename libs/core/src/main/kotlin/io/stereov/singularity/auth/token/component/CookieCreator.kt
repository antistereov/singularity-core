package io.stereov.singularity.auth.token.component

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.token.exception.CookieException
import io.stereov.singularity.auth.token.model.SecurityToken
import io.stereov.singularity.auth.token.model.SecurityTokenType
import io.stereov.singularity.global.properties.AppProperties
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant

/**
 * A component responsible for creating and clearing cookies based on security tokens.
 *
 * This class provides functionality to create cookies with appropriate attributes such as
 * name, value, expiration time, security constraints, and other properties based on the provided
 * [SecurityToken] and [SecurityTokenType]. It also allows clearing cookies by setting their max age to 0.
 */
@Component
class CookieCreator(
    private val appProperties: AppProperties
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Creates an HTTP cookie based on the provided security token and specified configurations.
     * The cookie includes attributes such as httpOnly, sameSite, path, and optionally secure and maxAge,
     * determined by the token's properties and application configurations.
     *
     * @param token the security token used to generate the cookie's value and name.
     * @param path the path within the site where the cookie is valid (defaults to "/").
     * @param sameSite the sameSite attribute of the cookie, preventing cross-origin requests (defaults to "Strict").
     * @return a [Result] containing the built [ResponseCookie] if creation succeeds, or [CookieException.Creation]
     *         if an error occurs during the creation process.
     */
    fun createCookie(
        token: SecurityToken<*>,
        path: String = "/",
        sameSite: String = "Strict"
    ): Result<ResponseCookie, CookieException.Creation> {
        logger.debug { "Creating cookie from token of type ${token.type.cookieName}" }

        return runCatching {
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

            cookie.build()
        }
            .mapError { ex -> CookieException.Creation("Failed to create ${token.type.cookieName}: ${ex.message}", ex) }
    }

    /**
     * Clears an HTTP cookie by creating a cookie with the same name, empty value, and a max age of 0.
     * This effectively invalidates the cookie on the client side.
     *
     * @param securityTokenType specifies the type of security token, which includes the cookie name to be cleared.
     * @param path the path within the site for which the cookie is valid. Defaults to "/".
     * @return a [Result] containing the built [ResponseCookie] if the cookie creation succeeds,
     *         or [CookieException.Creation] if an error occurs during the process.
     */
    suspend fun clearCookie(
        securityTokenType: SecurityTokenType,
        path: String = "/"
    ): Result<ResponseCookie, CookieException.Creation> {
        logger.debug { "Clearing cookie for ${securityTokenType.cookieName}" }

        return runCatching {
            val cookie = ResponseCookie.from(securityTokenType.cookieName, "")
                .httpOnly(true)
                .sameSite("Strict")
                .maxAge(0)
                .path(path)

            if (appProperties.secure) {
                cookie.secure(true)
            }

            cookie.build()
        }
            .mapError { ex ->
                CookieException.Creation(
                    "Failed to create ${securityTokenType.cookieName}: ${ex.message}",
                    ex
                )
            }
    }
}
