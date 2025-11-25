package io.stereov.singularity.auth.twofactor.model.token

import io.stereov.singularity.auth.token.model.SecurityToken
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.Jwt

data class TwoFactorAuthenticationToken(
    val userId: ObjectId,
    override val jwt: Jwt
) : SecurityToken<TwoFactorTokenType.Authentication>() {

    override val type = TwoFactorTokenType.Authentication
}
