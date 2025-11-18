package io.stereov.singularity.auth.core.service.token

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.token.EmailVerificationToken
import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.global.util.catchAs
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * A service responsible for creating and extracting email verification tokens.
 *
 * This service provides functionality for generating JWT tokens that verify user emails
 * and extracting specific email verification details from the token.
 *
 * @constructor Initializes the service with the provided JWT properties and service dependencies.
 * @param jwtProperties Configuration properties for token expiration and other JWT settings.
 * @param jwtService Service for encoding and decoding JWT tokens.
 */
@Service
class EmailVerificationTokenService(
    private val jwtProperties: JwtProperties,
    private val jwtService: JwtService,
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = "email_verification"

    /**
     * Creates an email verification token.
     *
     * This method generates a JWT token containing the user's email and a secret, with an issued and expiration date.
     *
     * @param userId The unique identifier of the user for whom the token is being created.
     * @param email The email address associated with the token.
     * @param secret A unique identifier or secret to include in the token.
     * @param issuedAt The timestamp when the token is issued. Defaults to the current time.
     *
     * @return A [Result] containing the encoded JWT token as a string if successful,
     * or a [TokenCreationException.Encoding] if an error occurs during token creation.
     */
    suspend fun create(
        userId: ObjectId,
        email: String,
        secret: String,
        issuedAt: Instant = Instant.now()
    ): Result<String, TokenCreationException.Encoding> = coroutineBinding {
        logger.debug { "Creating email verification token" }

        val claims = runCatching {
            JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
                .subject(userId.toHexString())
                .claim("email", email)
                .id(secret)
                .build()
        }
            .mapError { ex -> TokenCreationException.Encoding("Failed to build claim set: ${ex.message}", ex) }
            .bind()


        jwtService.encodeJwt(claims, tokenType)
            .map { token -> token.tokenValue }
            .bind()
    }

    /**
     * Extracts an [EmailVerificationToken] from the provided JWT token string.
     *
     * This method decodes and validates the token, extracting the user ID, email, and secret
     * to construct an [EmailVerificationToken].
     *
     * @param token The JWT token string to be decoded and validated.
     * @return A [Result] containing the extracted [EmailVerificationToken] if successful,
     * or a [TokenExtractionException] if an error occurs during extraction.
     */
    suspend fun extract(token: String): Result<EmailVerificationToken, TokenExtractionException> {
        logger.debug { "Validating email verification token" }

        return jwtService.decodeJwt(token, tokenType).andThen { jwt -> binding {
            val userId = jwt.subject
                .toResultOr { TokenExtractionException.Invalid("Email verification token does not contain sub claim") }
                .andThen { sub ->
                    catchAs({ ObjectId(sub) }) { ex ->
                        TokenExtractionException.Invalid("Invalid ObjectId in sub: $sub", ex)
                    }
                }.bind()

            val email = (jwt.claims["email"] as? String)
                .toResultOr { TokenExtractionException.Invalid("No email found in claims") }
                .bind()

            val secret = jwt.id
                .toResultOr { TokenExtractionException.Invalid("Email verification token does not contain ID") }
                .bind()


            EmailVerificationToken(userId, email, secret, jwt)
        } }
    }
}
