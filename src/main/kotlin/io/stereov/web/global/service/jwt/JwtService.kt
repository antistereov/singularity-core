package io.stereov.web.global.service.jwt

import io.stereov.web.global.service.jwt.exception.InvalidTokenException
import io.stereov.web.global.service.jwt.exception.TokenExpiredException
import io.stereov.web.global.service.jwt.model.AccessToken
import io.stereov.web.global.service.jwt.model.EmailVerificationToken
import io.stereov.web.global.service.jwt.model.RefreshToken
import io.stereov.web.properties.JwtProperties
import io.stereov.web.properties.MailProperties
import kotlinx.coroutines.reactive.awaitFirst
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
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

    private val tokenExpiresInSeconds = jwtProperties.expiresIn

    fun createAccessToken(userId: String, expiration: Long = tokenExpiresInSeconds): String {
        val jwsHeader = JwsHeader.with { "HS256" }.build()
        val claims = JwtClaimsSet.builder()
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(expiration))
            .subject(userId)
            .build()

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).tokenValue
    }

    suspend fun validateAndExtractAccessToken(token: String): AccessToken {
        val jwt = try {
            jwtDecoder.decode(token).awaitFirst()
        } catch(e: Exception) {
            throw InvalidTokenException("Cannot decode access token", e)
        }
        val expiresAt = jwt.expiresAt
            ?: throw InvalidTokenException("JWT does not contain expiration information")

        if (expiresAt <= Instant.now()) throw TokenExpiredException("Access token is expired")

        val accountId = jwt.subject
            ?: throw InvalidTokenException("JWT does not contain sub")

        return AccessToken(accountId)
    }

    fun createRefreshToken(accountId: String, deviceId: String): String {
        val jwsHeader = JwsHeader.with { "HS256" }.build()

        val claims = JwtClaimsSet.builder()
            .id(UUID.randomUUID().toString())
            .subject(accountId)
            .claim("device_id", deviceId)
            .issuedAt(Instant.now())
            .build()

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).tokenValue
    }

    suspend fun extractRefreshToken(refreshToken: String, deviceId: String): RefreshToken {
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
        val jwt = try {
            jwtDecoder.decode(token).awaitFirst()
        } catch (e: Exception) {
            throw InvalidTokenException("Cannot decode email verification token", e)
        }
        val expiresAt = jwt.expiresAt
            ?: throw InvalidTokenException("JWT does not contain expiration information")

        if (expiresAt <= Instant.now()) throw TokenExpiredException("Email verification token is expired")

        val email = jwt.subject
        val uuid = jwt.id

        return EmailVerificationToken(email, uuid)
    }
}
