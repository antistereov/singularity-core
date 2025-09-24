package io.stereov.singularity.auth.core.dto.response

import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod

data class AuthenticationStatusResponse(
    val authenticated: Boolean,
    val stepUp: Boolean,
    val emailVerified: Boolean?,
    val twoFactorRequired: Boolean,
    val preferredTwoFactorMethod: TwoFactorMethod?,
    val twoFactorMethods: List<TwoFactorMethod>?
)
