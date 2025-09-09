package io.stereov.singularity.auth.twofactor.model

import io.stereov.singularity.auth.core.model.Token
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.Jwt

data class TwoFactorInitSetupToken(
    val userId: ObjectId,
    val deviceId: String,
    override val jwt: Jwt
) : Token<TwoFactorTokenType.InitSetup> {

    override val type = TwoFactorTokenType.InitSetup
}