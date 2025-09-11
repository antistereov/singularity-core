package io.stereov.singularity.auth.core.model.token

import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.Jwt

data class RefreshToken(
    val userId: ObjectId,
    val sessionId: String,
    val tokenId: String,
    override val jwt: Jwt
) : SecurityToken<SessionTokenType.Refresh> {

    override val type = SessionTokenType.Refresh
}
