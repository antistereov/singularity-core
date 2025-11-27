package io.stereov.singularity.auth.token.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.token.exception.TotpSetupTokenCreationException
import io.stereov.singularity.auth.token.exception.TotpSetupTokenExtractionException
import io.stereov.singularity.auth.token.model.TotpSetupToken
import io.stereov.singularity.global.util.Constants
import io.swagger.v3.oas.annotations.servers.Server
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import java.time.Instant

/**
 * Service for handling the creation and validation of Time-based One-Time Password (TOTP) setup tokens.
 *
 * The purpose of this service is to facilitate two-factor authentication (2FA) setup via TOTP by creating
 * signed JSON Web Tokens (JWT) that encapsulate the necessary setup data, such as secrets and recovery codes.
 * The service also validates these setup tokens to ensure correctness and authenticity.
 */
@Server
class TotpSetupTokenService(
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = "totp_setup"

    /**
     * Creates a Time-based One-Time Password (TOTP) setup token for enabling two-factor authentication (2FA) for a user.
     *
     * The method generates JWT claims based on the provided inputs, including the user ID, secret, recovery codes,
     * and issued time. It then encodes these claims into a signed JWT, which encapsulates the TOTP setup information.
     *
     * @param userId The unique identifier of the user for whom the TOTP setup token is created.
     * @param secret The secret key used to generate time-based one-time passwords for 2FA.
     * @param recoveryCodes A list of recovery codes associated with the TOTP setup that can be used in case of
     *                      unavailability of the TOTP generator.
     * @param issuedAt The issuance time of the token. Defaults to the current time if not explicitly provided.
     * @return A [Result] that contains a [TotpSetupToken] on successful creation, or a [TotpSetupTokenCreationException]
     *         in case of an error during the creation process.
     */
    suspend fun create(
        userId: ObjectId,
        secret: String,
        recoveryCodes: List<String>,
        issuedAt: Instant = Instant.now()
    ): Result<TotpSetupToken, TotpSetupTokenCreationException> = coroutineBinding {
        logger.debug { "Creating setup token for 2fa" }

        val claims = runCatching {
            JwtClaimsSet.builder()
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
                .subject(userId.toHexString())
                .claim(Constants.TWO_FACTOR_SECRET_CLAIM, secret)
                .claim(Constants.TWO_FACTOR_RECOVERY_CLAIM, recoveryCodes)
                .build()
        }
            .mapError { ex -> TotpSetupTokenCreationException.Encoding("Failed to build claim set: ${ex.message}", ex) }
            .bind()


        jwtService.encodeJwt(claims, tokenType)
            .mapError { ex -> TotpSetupTokenCreationException.fromTokenCreationException(ex) }
            .map { jwt -> TotpSetupToken(secret, recoveryCodes, jwt) }
            .bind()
    }

    /**
     * Validates a two-factor authentication setup token for a given user.
     *
     * This method decodes the provided JWT token, validates its subject against the provided user ID,
     * and extracts the two-factor authentication secret and recovery codes from the token claims.
     * If validation or extraction fails, the corresponding exception is returned.
     *
     * @param token The JWT token to validate and extract data from.
     * @param userId The ID of the user for whom the token is being validated.
     * @return A [Result] containing a [TotpSetupToken] on success, or a [TotpSetupTokenExtractionException] on failure.
     */
    suspend fun validate(token: String, userId: ObjectId): Result<TotpSetupToken, TotpSetupTokenExtractionException> = coroutineBinding {
        logger.debug { "Validating two factor setup token" }

        val jwt = jwtService.decodeJwt(token, tokenType)
            .mapError { ex -> TotpSetupTokenExtractionException.fromTokenExtractionException(ex) }
            .bind()

        val subject = jwt.subject?.let { ObjectId(it) }

        if (subject != userId) {
            Err(TotpSetupTokenExtractionException.Invalid("Setup token is not valid for current user"))
                .bind()
        }

        val secret = (jwt.claims[Constants.TWO_FACTOR_SECRET_CLAIM] as? String)
            .toResultOr { TotpSetupTokenExtractionException.Invalid("JWT does not contain valid 2fa secret") }
            .bind()

        val recoveryCodes = (jwt.claims[Constants.TWO_FACTOR_RECOVERY_CLAIM] as? List<*>)
            .toResultOr { TotpSetupTokenExtractionException.Invalid("JWT does not contain valid 2fa recovery codes") }
            .bind()

        val recoveryCodeStrings = recoveryCodes
            .map { (it as? String)
                .toResultOr { TotpSetupTokenExtractionException.Invalid("Recovery codes contained in JWT cannot be casted to String") }
                .bind()
            }

        TotpSetupToken(secret, recoveryCodeStrings, jwt)
    }
}
