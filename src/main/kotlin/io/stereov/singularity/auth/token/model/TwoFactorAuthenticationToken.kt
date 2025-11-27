package io.stereov.singularity.auth.token.model

import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.Jwt

data class TwoFactorAuthenticationToken(
    val userId: ObjectId,
    override val jwt: Jwt
) : SecurityToken<TwoFactorTokenType.Authentication>() {

    override val type = TwoFactorTokenType.Authentication
}
