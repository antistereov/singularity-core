package io.stereov.singularity.auth.token.model

import io.stereov.singularity.global.model.Token
import org.springframework.security.oauth2.jwt.Jwt

data class TotpSetupToken(
    val secret: String,
    val recoveryCodes: List<String>,
    override val jwt: Jwt
) : Token()
