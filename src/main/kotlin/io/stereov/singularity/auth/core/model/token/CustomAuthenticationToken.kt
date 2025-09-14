package io.stereov.singularity.auth.core.model.token

import io.stereov.singularity.user.core.model.UserDocument
import org.bson.types.ObjectId
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.server.ServerWebExchange
import java.util.*

class CustomAuthenticationToken(
    val user: UserDocument,
    val sessionId: UUID,
    val tokenId: String,
    val exchange: ServerWebExchange,
    authorities: Collection<GrantedAuthority>
) : AbstractAuthenticationToken(authorities) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any? = null
    override fun getPrincipal(): ObjectId = user.id

    /**
     * Constructs a [CustomAuthenticationToken] from a [UserDocument] and a session ID.
     *
     * @param userDocument The [UserDocument] object containing user information.
     * @param sessionId The ID of the session.
     */
    constructor(userDocument: UserDocument, sessionId: UUID, tokenId: String, exchange: ServerWebExchange): this(
        user = userDocument,
        authorities = userDocument.sensitive.roles.map { SimpleGrantedAuthority("ROLE_$it") },
        sessionId = sessionId,
        tokenId = tokenId,
        exchange = exchange
    )
}
