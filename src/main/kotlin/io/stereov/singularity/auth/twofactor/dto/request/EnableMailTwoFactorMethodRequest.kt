package io.stereov.singularity.auth.twofactor.dto.request

data class EnableMailTwoFactorMethodRequest(
    val code: String
)