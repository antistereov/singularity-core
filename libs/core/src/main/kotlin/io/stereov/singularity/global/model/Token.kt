package io.stereov.singularity.global.model

import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

interface Token {
    val jwt: Jwt

    val expiresAt: Instant?
        get() = jwt.expiresAt

    val value: String
        get() = jwt.tokenValue
}