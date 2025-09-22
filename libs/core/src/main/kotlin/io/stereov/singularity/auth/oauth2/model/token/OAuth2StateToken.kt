package io.stereov.singularity.auth.oauth2.model.token

import io.stereov.singularity.global.model.Token
import org.springframework.security.oauth2.jwt.Jwt

data class OAuth2StateToken(
    val randomState: String,
    val sessionTokenValue: String?,
    val redirectUri: String?,
    val oauth2ProviderConnectionTokenValue: String?,
    val stepUp: Boolean,
    override val jwt: Jwt
) : Token