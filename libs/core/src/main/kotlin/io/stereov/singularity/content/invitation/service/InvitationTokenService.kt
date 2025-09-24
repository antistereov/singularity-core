package io.stereov.singularity.content.invitation.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.content.invitation.model.InvitationDocument
import io.stereov.singularity.content.invitation.model.InvitationToken
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.stereotype.Service

@Service
class InvitationTokenService(
    private val jwtService: JwtService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}
    private val tokenType = "invitation_token"

    suspend fun create(invitation: InvitationDocument): InvitationToken {
        logger.debug { "Creating invitation token" }

        val claims = JwtClaimsSet.builder()
            .issuedAt(invitation.issuedAt)
            .expiresAt(invitation.expiresAt)
            .subject(invitation.id.toString())
            .build()

        return InvitationToken(invitation.id, jwtService.encodeJwt(claims, tokenType))
    }

    suspend fun extract(token: String): InvitationToken {
        logger.debug { "Validating invitation token" }

        val jwt = jwtService.decodeJwt(token, tokenType)

        return InvitationToken(ObjectId(jwt.subject), jwt)
    }
}
