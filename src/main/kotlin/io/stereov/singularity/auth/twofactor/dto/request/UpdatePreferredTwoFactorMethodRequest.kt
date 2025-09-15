package io.stereov.singularity.auth.twofactor.dto.request

import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod

data class UpdatePreferredTwoFactorMethodRequest(
    val method: TwoFactorMethod
)
