package io.stereov.web.auth.model

import io.stereov.web.user.exception.InvalidUserDocumentException
import io.stereov.web.user.model.UserDocument
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

class CustomAuthenticationToken(
    val accountId: String,
    authorities: Collection<GrantedAuthority>
) : AbstractAuthenticationToken(authorities) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any? = null
    override fun getPrincipal(): String = accountId

    constructor(userDocument: UserDocument): this(
        accountId = userDocument.id ?: throw InvalidUserDocumentException("AccountDocument does not contain ID"),
        authorities = userDocument.roles.map { SimpleGrantedAuthority("ROLE_$it") },
    )
}
