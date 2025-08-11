package io.stereov.singularity.auth.model

import io.stereov.singularity.user.core.model.UserDocument
import org.bson.types.ObjectId
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

/**
 * # Custom authentication token for user authentication.
 *
 * This class extends the [AbstractAuthenticationToken] class and represents a custom authentication token
 * for user authentication.
 * It contains the user's ID, device-ID, and the user's authorities.
 *
 * @property user The [UserDocument] object containing user information.
 * @property deviceId The ID of the device.
 * @property authorities The collection of granted authorities for the user.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class CustomAuthenticationToken(
    val user: UserDocument,
    val deviceId: String,
    val tokenId: String,
    authorities: Collection<GrantedAuthority>
) : AbstractAuthenticationToken(authorities) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any? = null
    override fun getPrincipal(): ObjectId = user.id

    /**
     * Constructs a [CustomAuthenticationToken] from a [UserDocument] and a device ID.
     *
     * @param userDocument The [UserDocument] object containing user information.
     * @param deviceId The ID of the device.
     */
    constructor(userDocument: UserDocument, deviceId: String, tokenId: String): this(
        user = userDocument,
        authorities = userDocument.sensitive.roles.map { SimpleGrantedAuthority("ROLE_$it") },
        deviceId = deviceId,
        tokenId = tokenId
    )
}
