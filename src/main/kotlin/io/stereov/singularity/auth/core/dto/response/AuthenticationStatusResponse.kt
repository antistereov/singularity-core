package io.stereov.singularity.auth.core.dto.response

import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod

data class AuthenticationStatusResponse(
    val authorized: Boolean,
    val stepUp: Boolean,
    val twoFactorRequired: Boolean,
    val twoFactorMethods: List<TwoFactorMethod>?
)
