package io.stereov.singularity.auth.token.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.token.exception.PasswordResetTokenExtractionException
import io.stereov.singularity.auth.token.model.PasswordResetToken
import io.stereov.singularity.database.encryption.model.Encrypted
import io.stereov.singularity.database.encryption.service.EncryptionService
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Service class responsible for creating and validating [PasswordResetToken]s.
 *
 * This service provides functionality to:
 * - Generate password reset tokens for users that include encrypted secrets.
 * - Extract and validate password reset tokens, decrypting the necessary secrets in the process.
 *
 * The tokens generated are JWTs and include claims for securely handling user-specific data.
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
     * Creates a password reset token for a given user with an associated secret and issued timestamp.
     *
     * This method encrypts the provided secret and generates a JWT token containing relevant claims.
     *
     * @param userId The unique identifier of the user for whom the token is being created.
     * @param secret The secret that will be included in the token, encrypted before use.
     * @param issuedAt The timestamp indicating the issuance time of the token. Defaults to the current time if not provided.
     *
     * @return A [Result] wrapping the generated token as a [String] on success or a [TokenCreationException] on failure.
     */
    suspend fun create(
        userId: ObjectId,
        secret: String,
        issuedAt: Instant = Instant.now()
    ): Result<String, TokenCreationException> = coroutineBinding {
        logger.debug { "Creating password reset token" }

        val encryptedSecret = encryptionService.encrypt<String>(secret)
            .mapError { ex -> TokenCreationException.Secret("Failed to encrypt secret for password reset token: ${ex.message}", ex) }
            .bind()

        val claims = runCatching {
            JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
                .subject(userId.toHexString())
                .claim("secret", encryptedSecret.ciphertext)
                .claim("encryption-key-id", encryptedSecret.secretKey)
                .build()
        }
            .mapError { ex -> TokenCreationException.Encoding("Failed to build claim set for password reset token: ${ex.message}", ex) }
            .bind()

        jwtService.encodeJwt(claims, tokenType)
            .map { token -> token.tokenValue }
            .bind()
    }

    /**
     * Extracts and validates a password reset token from the provided JWT token string.
     *
     * This method decodes the JWT token, validates its claims, decrypts the secret
     * contained within the token, and constructs a [PasswordResetToken] containing
     * the extracted information.
     *
     * @param token The JWT token string to extract and validate.
     * @return A [Result] containing the extracted [PasswordResetToken] on success
     * or a [TokenExtractionException] on failure.
     */
    suspend fun extract(token: String): Result<PasswordResetToken, PasswordResetTokenExtractionException> {
        logger.debug { "Validating and extracting password reset token" }

        return jwtService.decodeJwt(token, tokenType)
            .mapError { ex -> PasswordResetTokenExtractionException.fromTokenExtractionException(ex) }
            .andThen { jwt ->
                coroutineBinding {
                    val userId = jwt.subject
                        .toResultOr { PasswordResetTokenExtractionException.Invalid("Password reset token does not contain sub") }
                        .andThen { sub ->
                            runCatching { ObjectId(sub) }
                                .mapError { ex -> PasswordResetTokenExtractionException.Invalid("Invalid ObjectId in sub: $sub", ex) }
                        }.bind()

                    val encryptedSecret = (jwt.claims["secret"] as? String)
                        .toResultOr { PasswordResetTokenExtractionException.Invalid("Password reset token does not contain secret claim") }
                        .bind()
                    val kid = (jwt.claims["encryption-key-id"] as? String)
                        .toResultOr { PasswordResetTokenExtractionException.Invalid("Password reset token does not contain key ID claim") }
                        .bind()

                    val secret = encryptionService.decrypt(Encrypted<String>(kid, encryptedSecret))
                        .mapError { ex -> PasswordResetTokenExtractionException.Secret("Failed to decrypt password reset secret: ${ex.message}", ex) }
                        .bind()

                    PasswordResetToken(userId, secret, jwt)
                }
        }


    }
}
