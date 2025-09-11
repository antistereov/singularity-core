package io.stereov.singularity.auth.twofactor.model.token

import io.stereov.singularity.global.model.Token
import org.springframework.security.oauth2.jwt.Jwt

data class TotpSetupToken(
    val secret: String,
    val recoveryCodes: List<String>,
    override val jwt: Jwt
) : Token
