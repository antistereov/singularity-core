package io.stereov.web.global.service.jwt

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.config.Constants
import io.stereov.web.global.service.jwt.exception.InvalidTokenException
import io.stereov.web.global.service.jwt.exception.TokenExpiredException
import io.stereov.web.global.service.jwt.model.AccessToken
import io.stereov.web.global.service.jwt.model.EmailVerificationToken
import io.stereov.web.global.service.jwt.model.RefreshToken
import io.stereov.web.properties.JwtProperties
import io.stereov.web.properties.MailProperties
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.security.oauth2.jwt.*
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*

@Service
class JwtService(
    private val jwtDecoder: ReactiveJwtDecoder,
    private val jwtEncoder: JwtEncoder,
    jwtProperties: JwtProperties,
    private val mailProperties: MailProperties,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    private val tokenExpiresInSeconds = jwtProperties.expiresIn

    fun createAccessToken(userId: String, deviceId: String, expiration: Long = tokenExpiresInSeconds): String {
        logger.debug { "Creating access token for user $userId and device $deviceId" }

        val jwsHeader = JwsHeader.with { "HS256" }.build()
        val claims = JwtClaimsSet.builder()
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(expiration))
            .subject(userId)
            .claim(Constants.JWT_DEVICE_CLAIM, deviceId)
            .build()

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).tokenValue
    }

    suspend fun validateAndExtractAccessToken(token: String): AccessToken {
        logger.debug { "Validating access token" }

        val jwt = decodeJwt(token)

        val accountId = jwt.subject
            ?: throw InvalidTokenException("JWT does not contain sub")

        val deviceId = jwt.claims[Constants.JWT_DEVICE_CLAIM] as? String
            ?: throw InvalidTokenException("JWT does not contain device id")

        return AccessToken(accountId, deviceId)
    }

    fun createRefreshToken(userId: String, deviceId: String): String {
        logger.debug { "Creating refresh token for user $userId and device $deviceId" }

        val jwsHeader = JwsHeader.with { "HS256" }.build()

        val claims = JwtClaimsSet.builder()
            .id(UUID.randomUUID().toString())
            .subject(userId)
            .claim("device_id", deviceId)
            .issuedAt(Instant.now())
            .build()

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).tokenValue
    }

    suspend fun extractRefreshToken(refreshToken: String, deviceId: String): RefreshToken {
        logger.debug { "Extracting refresh token" }

        val jwt = try {
            jwtDecoder.decode(refreshToken).awaitFirst()
        } catch (e: Exception) {
            throw InvalidTokenException("Cannot decode refresh token", e)
        }

        val accountId = jwt.subject
            ?: throw InvalidTokenException("Refresh token does not contain user id")

        return RefreshToken(accountId, deviceId, refreshToken)
    }

    fun createEmailVerificationToken(email: String, uuid: String): String {
        logger.debug { "Creating email verification token" }

        val jwsHeader = JwsHeader.with { "HS256" }.build()
        val claims = JwtClaimsSet.builder()
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(mailProperties.verificationExpiration))
            .subject(email)
            .id(uuid)
            .build()

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).tokenValue
    }

    suspend fun validateAndExtractVerificationToken(token: String): EmailVerificationToken {
        logger.debug { "Validating email verification token" }

        val jwt = decodeJwt(token)

        val email = jwt.subject
        val uuid = jwt.id

        return EmailVerificationToken(email, uuid)
    }

    fun createTwoFactorToken(userId: String, expiration: Long = tokenExpiresInSeconds): String {
        logger.debug { "Creating two factor token" }

        val jwsHeader = JwsHeader.with { "HS256" }.build()
        val claims = JwtClaimsSet.builder()
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(expiration))
            .subject(userId)
            .build()

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).tokenValue
    }

    suspend fun validateTwoFactorTokenAndExtractUserId(token: String): String {
        logger.debug { "Validating two factor token" }

        val jwt = decodeJwt(token)

        val userId = jwt.subject
            ?: throw InvalidTokenException("JWT does not contain sub")

        return userId
    }

    private suspend fun decodeJwt(token: String): Jwt {
        logger.debug { "Decoding jwt" }

        val jwt = try {
            jwtDecoder.decode(token).awaitFirst()
        } catch(e: Exception) {
            throw InvalidTokenException("Cannot decode access token", e)
        }
        val expiresAt = jwt.expiresAt
            ?: throw InvalidTokenException("JWT does not contain expiration information")

        if (expiresAt <= Instant.now()) throw TokenExpiredException("Token is expired")

        return jwt
    }
}
