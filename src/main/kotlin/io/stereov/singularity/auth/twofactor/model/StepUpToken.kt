package io.stereov.singularity.auth.twofactor.model

import io.stereov.singularity.auth.core.model.SecurityToken
import org.bson.types.ObjectId
import org.springframework.security.oauth2.jwt.Jwt

data class StepUpToken(
    val userId: ObjectId,
    val sessionId: String,
    override val jwt: Jwt
) : SecurityToken<TwoFactorTokenType.StepUp> {

    override val type = TwoFactorTokenType.StepUp
}
