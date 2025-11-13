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
 * Create an extract [EmailVerificationToken]s.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
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
     * This method generates a JWT token with the email and secret as claims.
     * The token is valid for the duration specified in the email properties.
     *
     * @param userId The ID of the user to create the token for.
     * @param email The email address to be verified.
     * @param secret The secret associated with the email.
     *
     * @return The generated JWT token.
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
     * Validates and extracts the email verification token.
     *
     * This method decodes the JWT token and extracts the email and secret claims.
     *
     * @param token The JWT token to be validated.
     *
     * @return An [EmailVerificationToken] object containing the email and secret.
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
