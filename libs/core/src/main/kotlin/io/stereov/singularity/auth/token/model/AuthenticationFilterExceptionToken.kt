package io.stereov.singularity.auth.token.model

import io.stereov.singularity.auth.token.exception.AccessTokenExtractionException
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
