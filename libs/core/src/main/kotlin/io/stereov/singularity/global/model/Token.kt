package io.stereov.singularity.global.model

import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

abstract class Token {
    abstract val jwt: Jwt

    val expiresAt: Instant?
        get() = jwt.expiresAt

    val value: String
        get() = jwt.tokenValue

    override fun toString(): String {
        return value
    }
}