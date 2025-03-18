package io.stereov.web.auth.model

import io.stereov.web.user.model.UserDocument
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

class CustomAuthenticationToken(
    val userId: String,
    val deviceId: String,
    authorities: Collection<GrantedAuthority>
) : AbstractAuthenticationToken(authorities) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any? = null
    override fun getPrincipal(): String = userId

    constructor(userDocument: UserDocument, deviceId: String): this(
        userId = userDocument.idX,
        authorities = userDocument.roles.map { SimpleGrantedAuthority("ROLE_$it") },
        deviceId = deviceId
    )
}
