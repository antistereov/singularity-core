package io.stereov.singularity.auth.core.model.token

import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.Jwt

data class StepUpToken(
    val userId: ObjectId,
    val sessionId: String,
    override val jwt: Jwt
) : SecurityToken<SessionTokenType.StepUp> {

    override val type = SessionTokenType.StepUp
}