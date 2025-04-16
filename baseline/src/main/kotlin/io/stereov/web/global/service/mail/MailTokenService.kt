package io.stereov.web.global.service.mail

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.global.service.jwt.exception.model.InvalidTokenException
import io.stereov.web.global.service.jwt.exception.model.TokenExpiredException
import io.stereov.web.global.service.mail.model.EmailVerificationToken
import io.stereov.web.global.service.mail.model.PasswordResetToken
import io.stereov.web.properties.MailProperties
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * # Service for managing email tokens.
 *
 * This service provides methods to create and validate email verification and password reset tokens.
 * It uses the [JwtService] to encode and decode JWT tokens.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class MailTokenService(
    private val mailProperties: MailProperties,
    private val jwtService: JwtService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Creates an email verification token.
     *
     * This method generates a JWT token with the email and secret as claims.
     * The token is valid for the duration specified in the mail properties.
     *
     * @param email The email address to be verified.
     * @param secret The secret associated with the email.
     *
     * @return The generated JWT token.
     */
    fun createVerificationToken(email: String, secret: String, issuedAt: Instant = Instant.now()): String {
        logger.debug { "Creating email verification token" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(mailProperties.verificationExpiration))
            .subject(email)
            .id(secret)
            .build()

        return jwtService.encodeJwt(claims)
    }

    /**
     * Validates and extracts the email verification token.
     *
     * This method decodes the JWT token and extracts the email and secret claims.
     *
     * @param token The JWT token to be validated.
     *
     * @return An [EmailVerificationToken] object containing the email and secret.
     *
     * @throws InvalidTokenException if the token is invalid or expired.
     * @throws TokenExpiredException if the token is expired.
     */
    suspend fun validateAndExtractVerificationToken(token: String): EmailVerificationToken {
        logger.debug { "Validating email verification token" }

        val jwt = jwtService.decodeJwt(token, true)

        val email = jwt.subject
        val secret = jwt.id

        return EmailVerificationToken(email, secret)
    }

    /**
     * Creates a password reset token.
     *
     * This method generates a JWT token with the user ID and secret as claims.
     * The token is valid for the duration specified in the mail properties.
     *
     * @param userId The ID of the user requesting the password reset.
     * @param secret The secret associated with the user.
     *
     * @return The generated JWT token.
     */
    fun createPasswordResetToken(userId: String, secret: String, issuedAt: Instant = Instant.now()): String {
        logger.debug { "Creating password reset token" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(mailProperties.passwordResetExpiration))
            .subject(userId)
            .id(secret)
            .build()
        
        return jwtService.encodeJwt(claims)
    }

    /**
     * Validates and extracts the password reset token.
     *
     * This method decodes the JWT token and extracts the user ID and secret claims.
     *
     * @param token The JWT token to be validated.
     *
     * @return A [PasswordResetToken] object containing the user ID and secret.
     *
     * @throws InvalidTokenException if the token is invalid or expired.
     * @throws TokenExpiredException if the token is expired.
     */
    suspend fun validateAndExtractPasswordResetToken(token: String): PasswordResetToken {
        logger.debug { "Validating and extracting password reset token" }

        val jwt = jwtService.decodeJwt(token, true)

        val userId = jwt.subject
        val secret = jwt.id

        return PasswordResetToken(userId, secret)
    }
}
