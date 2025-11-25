package io.stereov.singularity.auth.token.model

import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.Jwt
import java.util.*

data class StepUpToken(
    val userId: ObjectId,
    val sessionId: UUID,
    override val jwt: Jwt
) : SecurityToken<SessionTokenType.StepUp>() {

    override val type = SessionTokenType.StepUp
}
