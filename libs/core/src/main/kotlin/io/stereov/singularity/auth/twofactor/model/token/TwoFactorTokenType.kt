package io.stereov.singularity.auth.twofactor.model.token

import io.stereov.singularity.auth.token.model.SecurityTokenType

interface TwoFactorTokenType {

    object Authentication : SecurityTokenType {
        const val COOKIE_NAME = "two_factor_authentication_token"
        const val HEADER = "X-Two-Factor-Authentication-Token"

        override val header = HEADER
        override val cookieName = COOKIE_NAME
    }
}
