package io.stereov.singularity.auth.jwt.service

import com.github.michaelbull.result.*
import com.github.michaelbull.result.coroutines.coroutineBinding
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.jwt.exception.TokenCreationException
import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.security.oauth2.jwt.*
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Service responsible for handling encoding and decoding of JSON Web Tokens (JWT).
 * It uses the provided [ReactiveJwtDecoder] for decoding tokens, [JwtEncoder] for encoding tokens,
 * and [JwtSecretService] for fetching secure secrets necessary for signing and verifying JWTs.
 */
@Service
class JwtService(
    private val jwtDecoder: ReactiveJwtDecoder,
    private val jwtEncoder: JwtEncoder,
    private val jwtSecretService: JwtSecretService
) {

    private val logger = KotlinLogging.logger {}
    private val tokenTypeClaim = "token_type"

    /**
     * Decodes a JSON Web EmailVerificationTokenCreation (JWT) and validates its type and expiration details.
     *
     * @param token The JWT to decode.
     * @param tokenType The expected token type to validate against.
     *
     * @return A [Result] containing the decoded [Jwt] on success, or a [TokenExtractionException] on failure.
     */
    suspend fun decodeJwt(token: String, tokenType: String): Result<Jwt, TokenExtractionException> {
        logger.debug { "Decoding jwt" }

        val jwt = try {
            jwtDecoder.decode(token).awaitFirst()
        } catch(e: Exception) {
            logger.error(e) {}
            return Err(TokenExtractionException.Invalid("Cannot decode token", e))
        }

        if (jwt.claims[tokenTypeClaim] != tokenType)
            return Err(TokenExtractionException.Invalid("EmailVerificationTokenCreation is not of type $tokenType"))

        if (jwt.notBefore != null && jwt.notBefore > Instant.now()) {
            return Err(TokenExtractionException.Invalid("EmailVerificationTokenCreation not valid before ${jwt.notBefore}"))
        }

        val expiresAt = jwt.expiresAt
            ?: return Err(TokenExtractionException.Invalid("JWT does not contain expiration information"))

        if (expiresAt <= Instant.now()) return Err(TokenExtractionException.Expired("EmailVerificationTokenCreation is expired"))

        return Ok(jwt)
    }

    /**
     * Encodes a JSON Web EmailVerificationTokenCreation (JWT) using specified claims and token type.
     *
     * @param claims The set of claims to include in the JWT.
     * @param tokenType The type of token to associate with the JWT.
     * @return A [Result] containing the encoded [Jwt] on success, or a [TokenCreationException] on failure.
     */
    suspend fun encodeJwt(
        claims: JwtClaimsSet,
        tokenType: String
    ): Result<Jwt, TokenCreationException> = coroutineBinding {
        val currentJwt = jwtSecretService.getCurrentSecret()
            .mapError { ex -> TokenCreationException.Secret("Failed to fetch current JWT secret: ${ex.message}", ex) }
            .bind()

        val actualClaims = JwtClaimsSet.from(claims)
            .claim(tokenTypeClaim, tokenType)
            .build()

        val jwsHeader = JwsHeader
            .with { "HS256" }
            .keyId(currentJwt.key)
            .build()

        encodeJwt(JwtEncoderParameters.from(jwsHeader, actualClaims)).bind()
    }

    private fun encodeJwt(parameters: JwtEncoderParameters): Result<Jwt, TokenCreationException.Encoding> {
        return runCatching { jwtEncoder.encode(parameters) }
            .mapError { ex -> TokenCreationException.Encoding("Failed to encode jwt: ${ex.message}", ex) }
    }

}
