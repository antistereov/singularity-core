package io.stereov.singularity.auth.twofactor.model

import io.stereov.singularity.auth.core.model.TokenType

interface TwoFactorTokenType {

    object InitSetup : TokenType("two_factor_init_setup_token", "X-Two-Factor-Init-Setup-Token")
    object Setup : TokenType("two_factor_setup_token", "X-Two-Factor-Setup-Token")
    object Login : TokenType("two_factor_login_token", "X-Two-Factor-Login-Token")
    object StepUp : TokenType("step_up_token", "X-Step-Up-Token")

}