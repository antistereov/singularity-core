package io.stereov.singularity.auth.core.service.token

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.token.EmailVerificationToken
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.jwt.exception.model.TokenExpiredException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import java.time.Instant

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
     * This method generates a JWT token with the email and secret as claims.
     * The token is valid for the duration specified in the email properties.
     *
     * @param email The email address to be verified.
     * @param secret The secret associated with the email.
     *
     * @return The generated JWT token.
     */
    suspend fun create(userId: ObjectId, email: String, secret: String, issuedAt: Instant = Instant.now()): String {
        logger.debug { "Creating email verification token" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
            .subject(userId.toHexString())
            .claim("email", email)
            .id(secret)
            .build()

        return jwtService.encodeJwt(claims, tokenType).tokenValue
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
    suspend fun extract(token: String): EmailVerificationToken {
        logger.debug { "Validating email verification token" }

        val jwt = jwtService.decodeJwt(token, tokenType)

        val userId = ObjectId(jwt.subject)
        val email = jwt.claims["email"] as? String
            ?: throw InvalidTokenException("No email found in claims")
        val secret = jwt.id

        return EmailVerificationToken(userId, email, secret, jwt)
    }
}
