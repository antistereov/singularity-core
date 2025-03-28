package io.stereov.web.auth.model

import io.stereov.web.user.model.UserDocument
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

/**
 * # Custom authentication token for user authentication.
 *
 * This class extends the [AbstractAuthenticationToken] class and represents a custom authentication token
 *
 * for user authentication. It contains the user ID, device ID, and the user's authorities.
 *
 * @property userId The ID of the user.
 * @property deviceId The ID of the device.
 * @property authorities The collection of granted authorities for the user.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
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

    /**
     * Constructs a [CustomAuthenticationToken] from a [UserDocument] and a device ID.
     *
     * @param userDocument The [UserDocument] object containing user information.
     * @param deviceId The ID of the device.
     */
    constructor(userDocument: UserDocument, deviceId: String): this(
        userId = userDocument.idX,
        authorities = userDocument.roles.map { SimpleGrantedAuthority("ROLE_$it") },
        deviceId = deviceId
    )
}
