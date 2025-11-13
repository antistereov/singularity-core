package io.stereov.singularity.auth.core.model.token

import io.stereov.singularity.auth.core.exception.AccessTokenException
import org.springframework.security.authentication.AbstractAuthenticationToken

data class AccessTokenExceptionToken(
    val error: AccessTokenException
) : AbstractAuthenticationToken(null) {

    override fun getCredentials(): Any? {
        return null
    }

    override fun getPrincipal(): Any? {
        return null
    }
}
