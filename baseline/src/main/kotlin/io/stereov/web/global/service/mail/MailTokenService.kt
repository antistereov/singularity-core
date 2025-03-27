package io.stereov.web.global.service.mail

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.global.service.jwt.model.EmailVerificationToken
import io.stereov.web.properties.MailProperties
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class MailTokenService(
    private val mailProperties: MailProperties,
    private val jwtService: JwtService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    fun createToken(email: String, uuid: String): String {
        logger.debug { "Creating email verification token" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(mailProperties.verificationExpiration))
            .subject(email)
            .id(uuid)
            .build()

        return jwtService.encodeJwt(claims)
    }

    suspend fun validateAndExtractVerificationToken(token: String): EmailVerificationToken {
        logger.debug { "Validating email verification token" }

        val jwt = jwtService.decodeJwt(token, true)

        val email = jwt.subject
        val uuid = jwt.id

        return EmailVerificationToken(email, uuid)
    }
}
