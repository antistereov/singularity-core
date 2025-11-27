package io.stereov.singularity.auth.token.model

import io.stereov.singularity.global.model.Token
import org.springframework.security.oauth2.jwt.Jwt

data class OAuth2StateToken(
    val randomState: String,
    val redirectUri: String?,
    val stepUp: Boolean,
    override val jwt: Jwt,
) : Token()
