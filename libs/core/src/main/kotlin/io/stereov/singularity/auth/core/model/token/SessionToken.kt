package io.stereov.singularity.auth.core.model.token

import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import org.springframework.security.oauth2.jwt.Jwt

data class SessionToken(
    val id: String,
    val browser: String?,
    val os: String?,
    override val jwt: Jwt
) : SecurityToken<SessionTokenType.Session> {

    override val type = SessionTokenType.Session

    fun toSessionInfoRequest() = SessionInfoRequest(id, browser, os)
}
