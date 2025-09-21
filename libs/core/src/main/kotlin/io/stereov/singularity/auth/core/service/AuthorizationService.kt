package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.exception.model.InvalidPrincipalException
import io.stereov.singularity.auth.core.exception.model.NotAuthorizedException
import io.stereov.singularity.auth.core.model.token.CustomAuthenticationToken
import io.stereov.singularity.auth.core.model.token.ErrorAuthenticationToken
import io.stereov.singularity.auth.core.model.token.StepUpToken
import io.stereov.singularity.auth.core.service.token.StepUpTokenService
import io.stereov.singularity.auth.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.types.ObjectId
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.core.context.SecurityContext
import org.springframework.stereotype.Service
import java.util.*

@Service
class AuthorizationService(
    private val stepUpTokenService: StepUpTokenService,
    private val userService: UserService
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Check if the current user is authenticated.
     */
    @Suppress("UNUSED")
    suspend fun isAuthenticated(): Boolean {
        logger.debug { "Checking if user is authenticated" }

        return getCurrentUserIdOrNull() != null
    }

    /**
     * Get the ID of the currently authenticated user.
     *
     * @throws InvalidPrincipalException If the security context or authentication is missing.
     * @throws InvalidTokenException If the authentication does not contain the necessary properties.
     */
    suspend fun getCurrentUserId(): ObjectId {
        logger.debug {"Extracting user ID." }

        val auth = getCurrentAuthentication()
        return auth.userId
    }

    /**
     * Get the ID of the currently authenticated user or null if not authenticated.
     *
     * @throws InvalidPrincipalException If the security context or authentication is missing.
     * @throws InvalidTokenException If the authentication does not contain the necessary properties.
     */
    suspend fun getCurrentUserIdOrNull(): ObjectId? {
        return runCatching { getCurrentUserId() }
            .getOrNull()
    }

    /**
     * Get the ID of the currently authenticated user.
     *
     * @throws InvalidPrincipalException If the security context or authentication is missing.
     * @throws InvalidTokenException If the authentication does not contain the necessary properties.
     */
    suspend fun getCurrentUserOrNull(): UserDocument? {
        logger.debug { "Extracting current user" }

        return getCurrentUserIdOrNull()?.let { userService.findById(it) }
    }

    /**
     * Get the currently authenticated user document.
     *
     * @throws InvalidPrincipalException If the security context or authentication is missing.
     * @throws InvalidTokenException If the authentication does not contain the necessary properties.
     */
    suspend fun getCurrentUser(): UserDocument {
        logger.debug { "Extracting current user" }

        return userService.findById(getCurrentUserId())
    }

    /**
     * Get the ID of the currently authenticated session.
     *
     * @throws InvalidPrincipalException If the security context or authentication is missing.
     * @throws InvalidTokenException If the authentication does not contain the necessary properties.
     */
    suspend fun getCurrentSessionId(): UUID {
        logger.debug { "Extracting session ID" }

        val auth = getCurrentAuthentication()
        return auth.sessionId
    }

    /**
     * Get the ID of the currently authenticated session or null if not authenticated.
     *
     * @throws InvalidPrincipalException If the security context or authentication is missing.
     * @throws InvalidTokenException If the authentication does not contain the necessary properties.
     */
    suspend fun getCurrentSessionIdOrNull(): UUID? {
        return try {
            getCurrentSessionId()
        } catch(_: Exception) {
            null
        }
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

    /**
     * Require authentication.
     *
     * @throws InvalidPrincipalException If the security context or authentication is missing.
     * @throws InvalidTokenException If the authentication does not contain the necessary properties.
     */
    @Suppress("UNUSED")
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
        logger.debug { "Validating authorization: role $role" }

        val valid = getCurrentAuthentication().roles.contains(role)

        if (!valid) throw NotAuthorizedException("User does not have sufficient permission: User does not have role $role")
    }

    suspend fun requireGroupMembership(groupKey: String) {
        logger.debug { "Validating that the current user is part of the group \"$groupKey\"" }

        val auth = getCurrentAuthentication()

        if (!auth.groups.contains(groupKey) && !auth.roles.contains(Role.ADMIN)) {
            throw NotAuthorizedException("User does not have sufficient permission: User does not belong to group \"$groupKey\"")
        }
    }

    suspend fun getGroups(): Set<String> {
        return getCurrentAuthentication().groups
    }

    suspend fun requireStepUp(): StepUpToken {
        logger.debug { "Validating step up" }

        val authentication = getCurrentAuthentication()
        return stepUpTokenService.extract(authentication.exchange, authentication.userId, authentication.sessionId)
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
