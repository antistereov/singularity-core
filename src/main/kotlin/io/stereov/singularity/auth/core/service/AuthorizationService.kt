package io.stereov.singularity.auth.core.service

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.exception.AccessTokenException
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.exception.StepUpTokenException
import io.stereov.singularity.auth.core.model.token.AccessTokenExceptionToken
import io.stereov.singularity.auth.core.model.token.AuthenticationToken
import io.stereov.singularity.auth.core.model.token.StepUpToken
import io.stereov.singularity.auth.core.service.token.StepUpTokenService
import io.stereov.singularity.user.core.model.Role
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.types.ObjectId
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Service
import java.util.*

@Service
class AuthorizationService(
    private val stepUpTokenService: StepUpTokenService,
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Check if the current user is authenticated.
     */
    @Suppress("UNUSED")
    suspend fun isAuthenticated(): Boolean {
        logger.debug { "Checking if user is authenticated" }

        return getAuthentication().isOk
    }

    /**
     * Get the ID of the currently authenticated user.
     */
    suspend fun getUserId(): Result<ObjectId, AccessTokenException> {
        logger.debug { "Extracting user ID." }

        return getAuthentication().map { it.userId }
    }

    /**
     * Get the ID of the currently authenticated session.
     */
    suspend fun getSessionId(): Result<UUID, AccessTokenException> {
        logger.debug { "Extracting session ID" }

        return getAuthentication().map { it.sessionId }
    }

    /**
     * Get the ID of the currently authenticated token.
     */
    suspend fun getTokenId(): Result<String, AccessTokenException> {
        logger.debug { "Extracting token ID" }

        return getAuthentication().map { it.tokenId }
    }

    /**
     * Require authentication and return the current user's ID.
     */
    @Suppress("UNUSED")
    suspend fun requireAuthentication() = getUserId()

    /**
     * Validating that the current user has the required role.
     *
     * @param authentication The [AuthenticationToken] of the current request.
     * @param role The required role.
     */
    fun requireRole(authentication: AuthenticationToken, role: Role): Result<AuthenticationToken, AuthenticationException.RoleRequired> {
        logger.debug { "Validating authorization: role $role" }

        return when (authentication.roles.contains(role)) {
            true -> Ok(authentication)
            false -> Err(AuthenticationException.RoleRequired("Failed to perform this action: role ${role.name} is required"))
        }
    }

    /**
     * Validate the group membership of the current user.
     *
     * @param authentication The [AuthenticationToken] of the current request.
     * @param groupKey The key of the required group.
     */
    fun requireGroupMembership(authentication: AuthenticationToken, groupKey: String): Result<AuthenticationToken, AuthenticationException.GroupMembershipRequired> {
        logger.debug { "Checking if the current user is part of the group \"$groupKey\"" }

        return when (authentication.groups.contains(groupKey) || authentication.roles.contains(Role.ADMIN)) {
            true -> Ok(authentication)
            false -> Err(AuthenticationException.GroupMembershipRequired("Failed to perform this action: user must be a member of group $groupKey"))
        }
    }

    suspend fun getRoles(): Result<Set<Role>, AccessTokenException> {
        return getAuthentication().map { it.roles }
    }

    suspend fun getGroups(): Result<Set<String>, AccessTokenException> {
        return getAuthentication().map { it.groups }
    }

    /**
     * Validate that the current user performed a step-up.
     */
    suspend fun requireStepUp(authentication: AuthenticationToken): Result<StepUpToken, StepUpTokenException> {
        logger.debug { "Validating step up" }

        return stepUpTokenService.extract(
            authentication.exchange,
            authentication.userId,
            authentication.sessionId
        )
    }

    /**
     * Get the current authentication token.
     *
     * This method retrieves the current authentication token from the security context.
     */
    suspend fun getAuthentication(): Result<AuthenticationToken, AccessTokenException> {
        val authentication = ReactiveSecurityContextHolder.getContext()
            .awaitFirstOrNull()?.authentication
            ?: return Err(AccessTokenException.Missing())

        return when (authentication) {
            is AuthenticationToken -> Ok(authentication)
            is AccessTokenExceptionToken -> Err(authentication.error)
            else -> Err(AccessTokenException.Invalid(IllegalArgumentException("Unknown authentication type: ${authentication::class.simpleName}")))
        }
    }
}
