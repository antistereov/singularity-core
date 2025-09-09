package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.core.exception.model.InvalidPrincipalException
import io.stereov.singularity.auth.core.exception.model.NotAuthorizedException
import io.stereov.singularity.auth.core.model.CustomAuthenticationToken
import io.stereov.singularity.auth.core.model.ErrorAuthenticationToken
import io.stereov.singularity.auth.jwt.exception.TokenException
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.types.ObjectId
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.stereotype.Service

/**
 * # Service for managing authentication-related operations.
 *
 * This service provides methods to retrieve the current user's ID, user document, and device ID.
 *
 * It uses the [UserService] to interact with user data and the [ReactiveSecurityContextHolder]
 * to access the security context and authentication information.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Service
class AuthenticationService {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Get the ID of the currently authenticated user.
     *
     * @throws InvalidPrincipalException If the security context or authentication is missing.
     * @throws InvalidTokenException If the authentication does not contain the necessary properties.
     */
    suspend fun getCurrentUserId(): ObjectId {
        logger.debug {"Extracting user ID." }

        val auth = getCurrentAuthentication()
        return auth.user.id
    }

    suspend fun getCurrentUserOrNull(): UserDocument? {
        return try {
            getCurrentUser()
        } catch (_: TokenException) {
            null
        } catch (_: AuthException) {
            null
        }
    }

    /**
     * Get the currently authenticated user document.
     *
     * @throws InvalidPrincipalException If the security context or authentication is missing.
     * @throws InvalidTokenException If the authentication does not contain the necessary properties.
     */
    suspend fun getCurrentUser(): UserDocument {
        logger.debug { "Extracting current user" }

        return getCurrentAuthentication().user
    }

    /**
     * Get the ID of the currently authenticated device.
     *
     * @throws InvalidPrincipalException If the security context or authentication is missing.
     * @throws InvalidTokenException If the authentication does not contain the necessary properties.
     */
    suspend fun getCurrentDeviceId(): String {
        logger.debug { "Extracting device ID" }

        val auth = getCurrentAuthentication()
        return auth.deviceId
    }

    /**
     * Get the ID of the currently authenticated token.
     *
     * @throws InvalidPrincipalException If the security context or authentication is missing.
     * @throws InvalidTokenException If the authentication does not contain the necessary properties.
     */
    suspend fun getCurrentTokenId(): String {
        logger.debug { "Extracting token ID" }

        val auth = getCurrentAuthentication()
        return auth.tokenId
    }

    suspend fun requireAuthentication() {
        getCurrentAuthentication()
    }

    /**
     * Validate the authority of the current user.
     * It throws a [NotAuthorizedException] if the user does not have the required role.
     *
     * @param role The required role.
     *
     * @throws NotAuthorizedException If the user does not have the required role.
     */
    suspend fun requireRole(role: Role) {
        logger.debug { "Validating authorization" }

        val valid = this.getCurrentUser().sensitive.roles.contains(role)

        if (!valid) throw NotAuthorizedException("User does not have sufficient permission: User does not have role $role")
    }

    suspend fun requireGroupMembership(groupKey: String) {
        logger.debug { "Validating that the current user is part of the group \"$groupKey\"" }

        val user = getCurrentUser()

        if (!user.sensitive.groups.contains(groupKey) && !user.sensitive.roles.contains(Role.ADMIN)) {
            throw NotAuthorizedException("User does not have sufficient permission: User does not belong to group \"$groupKey\"")
        }
    }

    /**
     * Get the current authentication token.
     *
     * This method retrieves the current authentication token from the security context.
     *
     * @throws InvalidPrincipalException If the security context or authentication is missing.
     * @throws InvalidTokenException If the authentication does not contain the necessary properties.
     */
    private suspend fun getCurrentAuthentication(): CustomAuthenticationToken {
        val securityContext: SecurityContext = ReactiveSecurityContextHolder.getContext().awaitFirstOrNull()
            ?: throw InvalidPrincipalException("No security context found.")

        val authentication = securityContext.authentication
            ?: throw InvalidTokenException("Authentication is missing.")

        return when ( authentication) {
            is CustomAuthenticationToken -> authentication
            is ErrorAuthenticationToken -> throw authentication.error
            else -> throw InvalidTokenException("Unknown authentication type: ${authentication::class.simpleName}")
        }
    }
}
