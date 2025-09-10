package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.PasswordResetToken
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.jwt.exception.model.TokenExpiredException
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.database.encryption.service.EncryptionService
import io.stereov.singularity.mail.core.properties.MailProperties
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class PasswordResetTokenService(
    private val mailProperties: MailProperties,
    private val jwtService: JwtService,
    private val encryptionService: EncryptionService,
) {

    private val logger = KotlinLogging.logger {}

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
    suspend fun create(userId: ObjectId, secret: String, issuedAt: Instant = Instant.now()): String {
        logger.debug { "Creating password reset token" }

        val encryptedSecret = encryptionService.encrypt<String>(secret)

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(mailProperties.passwordResetExpiration))
            .subject(userId.toHexString())
            .claim("secret", encryptedSecret.ciphertext)
            .claim("encryption-key-id", encryptedSecret.secretKey)
            .build()

        return jwtService.encodeJwt(claims).tokenValue
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
    suspend fun extract(token: String): PasswordResetToken {
        logger.debug { "Validating and extracting password reset token" }

        val jwt = jwtService.decodeJwt(token, true)

        val userId = ObjectId(jwt.subject)
        val encryptedSecret = jwt.claims["secret"] as? String
            ?: throw InvalidTokenException("No secret found in claims")
        val kid = jwt.claims["encryption-key-id"] as? String
            ?: throw InvalidTokenException("No key ID found in claims")

        val secret = encryptionService.decrypt(Encrypted<String>(kid, encryptedSecret))

        return PasswordResetToken(userId, secret, jwt)
    }
}