package io.stereov.singularity.auth.twofactor.service.token

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.twofactor.model.token.TotpSetupToken
import io.stereov.singularity.global.util.Constants
import io.swagger.v3.oas.annotations.servers.Server
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import java.time.Instant

@Server
class TotpSetupTokenService(
    private val authorizationService: AuthorizationService,
    private val jwtService: JwtService,
    private val jwtProperties: JwtProperties,
) {

    private val logger = KotlinLogging.logger {}
    private val tokenType = "totp_setup"

    suspend fun create(userId: ObjectId, secret: String, recoveryCodes: List<String>, issuedAt: Instant = Instant.now()): TotpSetupToken {
        logger.debug { "Creating setup token for 2fa" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plusSeconds(jwtProperties.expiresIn))
            .subject(userId.toHexString())
            .claim(Constants.TWO_FACTOR_SECRET_CLAIM, secret)
            .claim(Constants.TWO_FACTOR_RECOVERY_CLAIM, recoveryCodes)
            .build()

        val jwt = jwtService.encodeJwt(claims, tokenType)

        return TotpSetupToken(secret, recoveryCodes, jwt)
    }

    suspend fun validate(token: String): TotpSetupToken {
        logger.debug { "Validating two factor setup token" }

        val jwt = jwtService.decodeJwt(token, tokenType)

        val userId = authorizationService.getCurrentUserId()
        val subject = jwt.subject?.let { ObjectId(it) }

        if (subject != userId) {
            throw InvalidTokenException("Setup token is not valid for current user")
        }

        val secret = jwt.claims[Constants.TWO_FACTOR_SECRET_CLAIM] as? String
            ?: throw InvalidTokenException("JWT does not contain valid 2fa secret")


        val recoveryCodes = jwt.claims[Constants.TWO_FACTOR_RECOVERY_CLAIM] as? List<*>
            ?: throw InvalidTokenException("JWT does not contain valid 2fa recovery codes")

        val recoveryCodeStrings = recoveryCodes
            .map { it as? String ?: throw InvalidTokenException("Recovery codes contained in JWT cannot be casted to String") }

        return TotpSetupToken(secret, recoveryCodeStrings, jwt)
    }
}