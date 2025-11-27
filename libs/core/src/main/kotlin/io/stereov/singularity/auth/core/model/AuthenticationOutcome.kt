package io.stereov.singularity.auth.core.model

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.principal.core.model.Role
import org.bson.types.ObjectId
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.*

/**
 * Represents the outcome of an authentication process.
 *
 * This sealed class provides different states that can result from an authentication attempt.
 * These states include successfully authenticated or no authentication.
 *
 * @param authorities A collection of granted authorities associated with this authentication outcome.
 */
sealed class AuthenticationOutcome(
    authorities: Collection<GrantedAuthority>,
) : AbstractAuthenticationToken(authorities) {
    override fun getCredentials() = null
    override fun isAuthenticated() = this is Authenticated

    protected val logger = KotlinLogging.logger {}
    abstract override fun getPrincipal(): ObjectId?

    fun requireAuthentication(): Result<Authenticated, AuthenticationException.AuthenticationRequired> {
        return if (this is Authenticated) {
            Ok(this)
        } else {
            Err(AuthenticationException.AuthenticationRequired("Failed to perform this action: authentication is required"))
        }
    }

    /**
     * Represents a successful authentication outcome.
     *
     * This class holds the details of an authenticated principal, including their identifier, roles, groups,
     * session ID, token ID, and the associated access token. It extends [AuthenticationOutcome]
     * and provides additional context about the authenticated user's abilities and session.
     *
     * @property principalId The unique identifier for the authenticated principal.
     * @property roles A set of roles assigned to the authenticated user.
     * @property groups A set of groups the authenticated user belongs to.
     * @property sessionId The session identifier associated with this authentication.
     * @property tokenId The token identifier for the user's access token.
     */
    open class Authenticated(
        val principalId: ObjectId,
        val roles: Set<Role>,
        val groups: Set<String>,
        val sessionId: UUID,
        val tokenId: String,
    ) : AuthenticationOutcome(
        authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") },
    ) {
        override fun getPrincipal(): ObjectId = principalId

        fun requireRole(role: Role): Result<Authenticated, AuthenticationException.RoleRequired> {
            logger.debug { "Validating authorization: role $role" }

            return when (roles.contains(role)) {
                true -> Ok(this)
                false -> Err(AuthenticationException.RoleRequired("Failed to perform this action: role ${role.value} is required"))
            }
        }

        fun requireGroupMembership(groupKey: String): Result<Authenticated, AuthenticationException.GroupMembershipRequired> {
            logger.debug { "Checking if the current user is part of the group \"$groupKey\"" }

            return when (groups.contains(groupKey) || roles.contains(Role.User.ADMIN)) {
                true -> Ok(this)
                false -> Err(AuthenticationException.GroupMembershipRequired("Failed to perform this action: user must be a member of group $groupKey"))
            }
        }
    }

    /**
     * Represents an authentication outcome where no principal is associated with the session.
     *
     * This class is a specific implementation of [AuthenticationOutcome] that indicates the absence
     * of a valid authenticated user or session, reflecting scenarios where authentication has failed
     * or is not applicable.
     *
     * Functionality:
     * - Overrides the `getPrincipal` method to return `null`, signaling that no authenticated
     *   principal exists for this outcome.
     *
     * Usage of this class is typically for cases where authentication results are required, but no
     * valid credentials or session information is present.
     */
    class None() : AuthenticationOutcome(emptySet()) {
        override fun getPrincipal() = null
    }
}
