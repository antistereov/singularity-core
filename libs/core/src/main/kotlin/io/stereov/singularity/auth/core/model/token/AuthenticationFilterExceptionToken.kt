package io.stereov.singularity.auth.core.model.token

import io.stereov.singularity.auth.core.exception.AccessTokenExtractionException
import org.springframework.security.authentication.AbstractAuthenticationToken

data class AuthenticationFilterExceptionToken(
    val error: AccessTokenExtractionException
) : AbstractAuthenticationToken(null) {

    override fun getCredentials(): Any? {
        return null
    }

    override fun getPrincipal(): Any? {
        return null
    }
}
