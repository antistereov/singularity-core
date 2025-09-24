package io.stereov.singularity.auth.twofactor.dto.request

import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod

data class ChangePreferredTwoFactorMethodRequest(
    val method: TwoFactorMethod
)
