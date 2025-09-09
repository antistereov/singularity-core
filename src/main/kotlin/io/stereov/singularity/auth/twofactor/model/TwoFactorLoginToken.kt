package io.stereov.singularity.auth.twofactor.model

import io.stereov.singularity.auth.core.model.SecurityToken
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.Jwt

data class TwoFactorLoginToken(
    val userId: ObjectId,
    override val jwt: Jwt
) : SecurityToken<TwoFactorTokenType.Login> {

    override val type = TwoFactorTokenType.Login
}
