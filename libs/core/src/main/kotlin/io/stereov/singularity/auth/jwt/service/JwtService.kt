package io.stereov.singularity.auth.jwt.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.jwt.exception.model.TokenCreationException
import io.stereov.singularity.auth.jwt.exception.model.TokenException
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.security.oauth2.jwt.*
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * JWT Service
 *
 * This service is responsible for encoding and decoding JWT tokens.
 * It uses the `ReactiveJwtDecoder` and `JwtEncoder` to handle JWT operations.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class JwtService(
    private val jwtDecoder: ReactiveJwtDecoder,
    private val jwtEncoder: JwtEncoder,
    private val jwtSecretService: JwtSecretService
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    private val tokenTypeClaim = "token_type"

    /**
     * Decodes a JWT token.
     *
     * This method decodes the JWT token and checks for expiration if specified.
     *
     * @param token The JWT token to decode.
     * @param tokenType The type of token.
     */
    suspend fun decodeJwt(token: String, tokenType: String): Result<Jwt, TokenException> {
        logger.debug { "Decoding jwt" }

        val jwt = try {
            jwtDecoder.decode(token).awaitFirst()
        } catch(e: Exception) {
            logger.error(e) {}
            return  Err(TokenException.Invalid("Cannot decode token", e))
        }

        if (jwt.claims[tokenTypeClaim] != tokenType)
            return Err(TokenException.Invalid("Token is not of type $tokenType"))

        if (jwt.notBefore != null && jwt.notBefore > Instant.now()) {
            return Err(TokenException.Invalid("Token not valid before ${jwt.notBefore}"))
        }

        val expiresAt = jwt.expiresAt
            ?: return Err(TokenException.Invalid("JWT does not contain expiration information"))

        if (expiresAt <= Instant.now()) return Err(TokenException.Expired("Token is expired"))

        return Ok(jwt)
    }

    /**
     * Encodes a JWT token.
     *
     * This method encodes the JWT token using the provided claims.
     *
     * @param claims The claims to include in the JWT.
     * @param tokenType The type of token.
     *
     * @return The encoded JWT token as a string.
     */
    suspend fun encodeJwt(claims: JwtClaimsSet, tokenType: String): Result<Jwt, TokenCreationException.Encoding> {
        val currentJwt = jwtSecretService.getCurrentSecret()

        val actualClaims = JwtClaimsSet.from(claims)
            .claim(tokenTypeClaim, tokenType)
            .build()

        val jwsHeader = JwsHeader
            .with { "HS256" }
            .keyId(currentJwt.key)
            .build()

        return this.encodeJwt(JwtEncoderParameters.from(jwsHeader, actualClaims))
    }

    /**
     * Encode JWT based on encoder parameters.
     *
     * @param parameters Encoder parameters.
     */
    private fun encodeJwt(parameters: JwtEncoderParameters): Result<Jwt, TokenCreationException.Encoding> {
        return try {
            Ok(jwtEncoder.encode(parameters))
        } catch(e: JwtEncodingException) {
            Err(TokenCreationException.Encoding("Failed to encode jwt: ${e.message}", e))
        }
    }
}
