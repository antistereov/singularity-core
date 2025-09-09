package io.stereov.singularity.auth.session.model

import io.stereov.singularity.auth.core.model.Token
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.Jwt

data class RefreshToken(
    val userId: ObjectId,
    val deviceId: String,
    val tokenId: String,
    override val jwt: Jwt
) : Token<SessionTokenType.Refresh> {

    override val type = SessionTokenType.Refresh
}
