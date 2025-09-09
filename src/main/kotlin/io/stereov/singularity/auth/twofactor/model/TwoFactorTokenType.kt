package io.stereov.singularity.auth.twofactor.model

import io.stereov.singularity.auth.core.model.SecurityTokenType

interface TwoFactorTokenType {

    object InitSetup : SecurityTokenType {
        const val HEADER = "X-Two-Factor-Init-Setup-Token"
        const val COOKIE_NAME = "two_factor_init_setup_token"

        override val header = HEADER
        override val cookieName = COOKIE_NAME
    }
    object Login : SecurityTokenType {
        const val COOKIE_NAME = "two_factor_login_token"
        const val HEADER = "X-Two-Factor-Login-Token"

        override val header = HEADER
        override val cookieName = COOKIE_NAME
    }
    object StepUp : SecurityTokenType {
        const val COOKIE_NAME = "step_up_token"
        const val HEADER = "X-Step-Up-Token"

        override val header = HEADER
        override val cookieName = COOKIE_NAME
    }

}
