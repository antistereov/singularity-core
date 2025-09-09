package io.stereov.singularity.auth.twofactor.model

import io.stereov.singularity.auth.core.model.Token
import org.springframework.security.oauth2.jwt.Jwt

data class TwoFactorSetupToken(
    val secret: String,
    val recoveryCodes: List<String>,
    override val jwt: Jwt
) : Token
