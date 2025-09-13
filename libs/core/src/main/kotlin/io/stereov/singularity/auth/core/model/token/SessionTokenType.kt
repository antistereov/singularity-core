package io.stereov.singularity.auth.core.model.token

import org.springframework.http.HttpHeaders

object SessionTokenType {

    object Access : SecurityTokenType {
        const val HEADER = HttpHeaders.AUTHORIZATION
        const val COOKIE_NAME = "access_token"

        override val header = HEADER
        override val cookieName = COOKIE_NAME
    }
    object Refresh : SecurityTokenType {
        const val HEADER = "X-Refresh-Token"
        const val COOKIE_NAME = "refresh_token"

        override val header = HEADER
        override val cookieName = COOKIE_NAME
    }
    object StepUp : SecurityTokenType {
        const val COOKIE_NAME = "step_up_token"
        const val HEADER = "X-Step-Up-Token"

        override val header = HEADER
        override val cookieName = COOKIE_NAME
    }
    object Session : SecurityTokenType {
        const val COOKIE_NAME = "session_token"
        const val HEADER = "X-Session-Token"

        override val header = HEADER
        override val cookieName = COOKIE_NAME
    }
}
