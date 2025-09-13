package io.stereov.singularity.auth.oauth2.model.token

import io.stereov.singularity.auth.core.model.token.SecurityToken
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.Jwt

data class OAuth2ProviderConnectionToken(
    val userId: ObjectId,
    val sessionId: String,
    val provider: String,
    override val jwt: Jwt
) : SecurityToken<OAuth2TokenType.ProviderConnection> {

    override val type = OAuth2TokenType.ProviderConnection
}
