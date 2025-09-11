package io.stereov.singularity.content.invitation.model

import io.stereov.singularity.global.model.Token
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.Jwt

data class InvitationToken(
    val invitationId: ObjectId,
    override val jwt: Jwt
) : Token
