package io.stereov.singularity.auth.core.model

import org.springframework.http.HttpHeaders

interface SessionTokenType {

    object Access : SecurityTokenType {
        const val HEADER = HttpHeaders.AUTHORIZATION
        const val COOKIE_NAME = "access_token"

        override val header = HEADER
        override val cookieName = COOKIE_NAME
    }
    object Refresh : SecurityTokenType {
        const val HEADER = HttpHeaders.AUTHORIZATION
        const val COOKIE_NAME = "refresh_token"

        override val header = HEADER
        override val cookieName = COOKIE_NAME
    }
}
