package io.stereov.singularity.auth.twofactor.dto.response

import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod

data class TwoFactorStatusResponse(
    val twoFactorRequired: Boolean,
    val authorized: Boolean,
    val stepUp: Boolean,
    val allowedMethods: List<TwoFactorMethod>?
)
