package io.stereov.singularity.auth.core.model.token

import io.stereov.singularity.auth.core.dto.request.SessionInfoRequest
import org.springframework.security.oauth2.jwt.Jwt
import java.util.*

data class SessionToken(
    val browser: String?,
    val os: String?,
    val locale: Locale?,
    override val jwt: Jwt
) : SecurityToken<SessionTokenType.Session>() {

    override val type = SessionTokenType.Session

    fun toSessionInfoRequest() = SessionInfoRequest(browser, os)
}