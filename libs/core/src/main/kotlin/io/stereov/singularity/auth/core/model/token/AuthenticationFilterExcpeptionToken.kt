package io.stereov.singularity.auth.core.model.token

import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import org.springframework.security.authentication.AbstractAuthenticationToken

data class AuthenticationFilterExcpeptionToken(
    val error: TokenExtractionException
) : AbstractAuthenticationToken(null) {

    override fun getCredentials(): Any? {
        return null
    }

    override fun getPrincipal(): Any? {
        return null
    }
}
