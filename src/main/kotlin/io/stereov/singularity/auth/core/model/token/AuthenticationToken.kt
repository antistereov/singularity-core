package io.stereov.singularity.auth.core.model.token

import io.stereov.singularity.user.core.model.Role
import org.bson.types.ObjectId
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.server.ServerWebExchange
import java.util.*

class AuthenticationToken(
    val userId: ObjectId,
    val roles: Set<Role>,
    val groups: Set<String>,
    val sessionId: UUID,
    val tokenId: String,
    val exchange: ServerWebExchange,
    authorities: Collection<GrantedAuthority>
) : AbstractAuthenticationToken(authorities) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any? = null
    override fun getPrincipal(): ObjectId = userId

    constructor(
        userId: ObjectId,
        roles: Set<Role>,
        groups: Set<String>,
        sessionId: UUID,
        tokenId: String,
        exchange: ServerWebExchange
    ): this(
        userId = userId,
        roles = roles,
        groups = groups,
        authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") },
        sessionId = sessionId,
        tokenId = tokenId,
        exchange = exchange
    )
}
