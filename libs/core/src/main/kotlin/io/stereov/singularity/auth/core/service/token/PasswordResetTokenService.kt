package io.stereov.singularity.auth.core.service.token

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.token.PasswordResetToken
import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.database.encryption.service.EncryptionService
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Create an extract [PasswordResetToken]s.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class PasswordResetTokenService(
    private val jwtProperties: JwtProperties,
    private val jwtService: JwtService,
    private val encryptionService: EncryptionService,
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = "password_reset"

    /**
     * Creates a password reset token.
     *
     * This method generates a JWT token with the user ID and secret as claims.
     * The token is valid for the duration specified in the email properties.
     *
     * @param userId The ID of the user requesting the password reset.
     * @param secret The secret associated with the user.
     *
     * @return The generated JWT token.
     */
    suspend fun create(
        userId: ObjectId,
        secret: String,
        issuedAt: Instant = Instant.now()
    ): Result<String, TokenCreationException.Encoding> = coroutineBinding {
        logger.debug { "Creating password reset token" }

        val encryptedSecret = encryptionService.encrypt<String>(secret)

        val claims = runCatching {
            JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
                .subject(userId.toHexString())
                .claim("secret", encryptedSecret.ciphertext)
                .claim("encryption-key-id", encryptedSecret.secretKey)
                .build()
        }
            .mapError { ex -> TokenCreationException.Encoding("Failed to build claim set: ${ex.message}", ex) }
            .bind()

        jwtService.encodeJwt(claims, tokenType)
            .map { token -> token.tokenValue }
            .bind()
    }

    /**
     * Validates and extracts the password reset token.
     *
     * This method decodes the JWT token and extracts the user ID and secret claims.
     *
     * @param token The JWT token to be validated.
     *
     * @return A [PasswordResetToken] object containing the user ID and secret.
     */
    suspend fun extract(token: String): Result<PasswordResetToken, TokenExtractionException> {
        logger.debug { "Validating and extracting password reset token" }

        return jwtService.decodeJwt(token, tokenType).andThen { jwt ->
            coroutineBinding {
                val userId = jwt.subject
                    .toResultOr { TokenExtractionException.Invalid("Password reset token does not contain sub") }
                    .andThen { sub ->
                        runCatching { ObjectId(sub) }
                            .mapError { ex -> TokenExtractionException.Invalid("Invalid ObjectId in sub: $sub", ex) }
                    }.bind()

                val encryptedSecret = (jwt.claims["secret"] as? String)
                    .toResultOr { TokenExtractionException.Invalid("Password reset token does not contain secret claim") }
                    .bind()
                val kid = (jwt.claims["encryption-key-id"] as? String)
                    .toResultOr { TokenExtractionException.Invalid("Password reset token does not contain key ID claim") }
                    .bind()

                val secret = encryptionService.decrypt(Encrypted<String>(kid, encryptedSecret))

                PasswordResetToken(userId, secret, jwt)
            }
        }


    }
}
