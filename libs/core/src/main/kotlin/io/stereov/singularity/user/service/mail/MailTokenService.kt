package io.stereov.singularity.user.service.mail

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.encryption.model.Encrypted
import io.stereov.singularity.encryption.service.EncryptionService
import io.stereov.singularity.jwt.service.JwtService
import io.stereov.singularity.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.jwt.exception.model.TokenExpiredException
import io.stereov.singularity.mail.model.EmailVerificationToken
import io.stereov.singularity.mail.model.PasswordResetToken
import io.stereov.singularity.mail.properties.MailProperties
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

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
    private val encryptionService: EncryptionService,
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
    suspend fun createVerificationToken(userId: ObjectId, email: String, secret: String, issuedAt: Instant = Instant.now()): String {
        logger.debug { "Creating email verification token" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(mailProperties.verificationExpiration))
            .subject(userId.toHexString())
            .claim("email", email)
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

        val userId = ObjectId(jwt.subject)
        val email = jwt.claims["email"] as? String
            ?: throw InvalidTokenException("No email found in claims")
        val secret = jwt.id

        return EmailVerificationToken(userId, email, secret)
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
    suspend fun createPasswordResetToken(userId: ObjectId, secret: String, issuedAt: Instant = Instant.now()): String {
        logger.debug { "Creating password reset token" }

        val encryptedSecret = encryptionService.encrypt<String>(secret)

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(mailProperties.passwordResetExpiration))
            .subject(userId.toHexString())
            .claim("secret", encryptedSecret.ciphertext)
            .claim("encryption-key-id", encryptedSecret.secretId)
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

        val userId = ObjectId(jwt.subject)
        val encryptedSecret = jwt.claims["secret"] as? String
            ?: throw InvalidTokenException("No secret found in claims")
        val kid = jwt.claims["encryption-key-id"] as? String
            ?: throw InvalidTokenException("No key ID found in claims")

        val secret = encryptionService.decrypt(Encrypted<String>(UUID.fromString(kid), encryptedSecret))

        return PasswordResetToken(userId, secret)
    }
}
