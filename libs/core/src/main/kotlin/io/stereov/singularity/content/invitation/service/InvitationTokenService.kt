package io.stereov.singularity.content.invitation.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.content.invitation.model.InvitationDocument
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service

@Service
class InvitationTokenService(
    private val jwtService: JwtService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun createInvitationToken(invitation: InvitationDocument): String {
        logger.debug { "Creating invitation token" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(invitation.issuedAt)
            .expiresAt(invitation.expiresAt)
            .subject(invitation.id.toString())
            .build()

        return jwtService.encodeJwt(claims).tokenValue
    }

    suspend fun validateInvitationTokenAndGetId(token: String): ObjectId {
        logger.debug { "Validating invitation token" }

        val jwt = jwtService.decodeJwt(token, true)

        return ObjectId(jwt.subject)
    }
}
