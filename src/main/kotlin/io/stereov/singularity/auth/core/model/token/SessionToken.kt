package io.stereov.singularity.auth.core.model.token

import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import io.stereov.singularity.global.model.Token
import org.springframework.security.oauth2.jwt.Jwt

data class SessionToken(
    val id: String,
    val browser: String?,
    val os: String?,
    override val jwt: Jwt
) : Token {

    fun toSessionInfoRequest() = SessionInfoRequest(id, browser, os)
}
