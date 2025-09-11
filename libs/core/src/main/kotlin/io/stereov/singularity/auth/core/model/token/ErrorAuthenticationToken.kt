package io.stereov.singularity.auth.core.model.token

import org.springframework.security.authentication.AbstractAuthenticationToken

data class ErrorAuthenticationToken(
    val error: Throwable
) : AbstractAuthenticationToken(null) {

    override fun getCredentials(): Any? {
        return null
    }

    override fun getPrincipal(): Any? {
        return null
    }
}
