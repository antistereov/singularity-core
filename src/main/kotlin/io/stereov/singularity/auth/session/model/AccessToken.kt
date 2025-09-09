package io.stereov.singularity.auth.session.model

import io.stereov.singularity.auth.core.model.SecurityToken
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.Jwt

data class AccessToken(
    val userId: ObjectId,
    val deviceId: String,
    val tokenId: String,
    override val jwt: Jwt
) : SecurityToken<SessionTokenType.Access> {

    override val type = SessionTokenType.Access
}
