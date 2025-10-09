package io.stereov.singularity.auth.core.dto.response

import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod

data class StepUpResponse(
    val stepUpToken: String?,
    val twoFactorRequired: Boolean,
    val twoFactorMethods: List<TwoFactorMethod>?,
    val preferredTwoFactorMethod: TwoFactorMethod?,
    val twoFactorAuthenticationToken: String?,
)
