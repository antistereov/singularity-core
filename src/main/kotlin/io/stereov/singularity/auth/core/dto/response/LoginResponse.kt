package io.stereov.singularity.auth.core.dto.response

import io.stereov.singularity.auth.geolocation.dto.GeolocationResponse
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod
import io.stereov.singularity.principal.core.dto.response.PrincipalResponse

data class LoginResponse(
    val user: PrincipalResponse,
    val accessToken: String?,
    val refreshToken: String?,
    val twoFactorRequired: Boolean,
    val twoFactorMethods: List<TwoFactorMethod>?,
    val preferredTwoFactorMethod: TwoFactorMethod?,
    val twoFactorAuthenticationToken: String?,
    val location: GeolocationResponse?
)
