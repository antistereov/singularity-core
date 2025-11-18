package io.stereov.singularity.auth.core.service

import com.github.michaelbull.result.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.exception.AccessTokenExtractionException
import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.core.model.token.AuthenticationFilterExceptionToken
import io.stereov.singularity.auth.core.model.token.AuthenticationToken
import io.stereov.singularity.auth.core.model.token.StepUpToken
import io.stereov.singularity.auth.core.service.token.StepUpTokenService
import io.stereov.singularity.auth.jwt.exception.TokenExtractionException
import io.stereov.singularity.user.core.model.Role
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.types.ObjectId
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Service
import java.util.*

/**
 * A service responsible for managing and validating user authentication and authorization.
 *
 * This service provides methods for validating user authentication, retrieving user details
 * such as user ID, session ID, access token ID, roles, and groups. It also includes utilities
 * for enforcing access control checks such as role requirements, group membership, and step-up
 * authentication validation.
 *
 * @constructor Initializes the service with the required dependencies.
 * @param stepUpTokenService A service for managing step-up authentication tokens.
 */
@Service
class AuthorizationService(
    private val stepUpTokenService: StepUpTokenService,
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Checks whether the current user is authenticated.
     *
     * @return A boolean indicating whether the user is authenticated.
     */
    @Suppress("UNUSED")
    suspend fun isAuthenticated(): Boolean {
        logger.debug { "Checking if user is authenticated" }

        return getAuthentication().isOk
    }

    /**
     * Retrieves the user ID of the currently authenticated user.
     *
     * @return A [Result] containing the [ObjectId] of the user if authentication is successful,
     *   or an [AccessTokenExtractionException] if an error occurs during authentication.
     */
    suspend fun getUserId(): Result<ObjectId, AccessTokenExtractionException> {
        logger.debug { "Extracting user ID." }

        return getAuthentication().map { it.userId }
    }

    /**
     * Extracts the session ID of the currently authenticated user.
     *
     * @return A [Result] containing the [UUID] of the session if the user is authenticated,
     *   or an [AccessTokenExtractionException] if an error occurs during authentication or token extraction.
     */
    suspend fun getSessionId(): Result<UUID, AccessTokenExtractionException> {
        logger.debug { "Extracting session ID" }

        return getAuthentication().map { it.sessionId }
    }

    /**
     * Extracts the access token ID of the currently authenticated user.
     *
     * @return A [Result] containing the token ID as a [String] if the authentication is successful,
     *   or an [AccessTokenExtractionException] if an error occurs during authentication or token extraction.
     */
    suspend fun getAccessTokenId(): Result<String, AccessTokenExtractionException> {
        logger.debug { "Extracting token ID" }

        return getAuthentication().map { it.tokenId }
    }

    /**
     * Ensures that the currently authenticated user is valid and retrieves the associated user ID.
     *
     * This method invokes `getUserId()` to verify the authentication status of the user.
     * If the authentication is successful, the user's ID is returned.
     *
     * @return A [Result] containing the [ObjectId] of the authenticated user if the authentication is valid,
     * or an [AccessTokenExtractionException] if an error occurs during authentication.
     */
    @Suppress("UNUSED")
    suspend fun requireAuthentication() = getUserId()

    /**
     * Verifies that the provided authentication token includes the specified role.
     *
     * @param authentication The [AuthenticationToken] containing the user's roles.
     * @param role The [Role] required for the action.
     * @return A [Result] containing the [AuthenticationToken] if the required role is present,
     * or an [AuthenticationException.RoleRequired] if the required role is missing.
     */
    fun requireRole(
        authentication: AuthenticationToken,
        role: Role
    ): Result<AuthenticationToken, AuthenticationException.RoleRequired> {
        logger.debug { "Validating authorization: role $role" }

        return when (authentication.roles.contains(role)) {
            true -> Ok(authentication)
            false -> Err(AuthenticationException.RoleRequired("Failed to perform this action: role ${role.name} is required"))
        }
    }

    /**
     * Ensures that the user associated with the given authentication token is a member of the specified group,
     * or holds the [Role.ADMIN] role.
     *
     * @param authentication The [AuthenticationToken] representing the authenticated user's session and associated roles/groups.
     * @param groupKey The key identifying the required group the user must belong to.
     * @return A [Result] containing the [AuthenticationToken] if the user is a member of the group or holds the ADMIN role,
     * or an [AuthenticationException.GroupMembershipRequired] if the user does not meet the group membership requirement.
     */
    fun requireGroupMembership(
        authentication: AuthenticationToken,
        groupKey: String
    ): Result<AuthenticationToken, AuthenticationException.GroupMembershipRequired> {
        logger.debug { "Checking if the current user is part of the group \"$groupKey\"" }

        return when (authentication.groups.contains(groupKey) || authentication.roles.contains(Role.ADMIN)) {
            true -> Ok(authentication)
            false -> Err(AuthenticationException.GroupMembershipRequired("Failed to perform this action: user must be a member of group $groupKey"))
        }
    }

    /**
     * Retrieves the roles associated with the currently authenticated user.
     *
     * @return A [Result] containing a [Set] of [Role] objects if authentication is successful,
     * or an [AccessTokenExtractionException] if an error occurs during authentication or role retrieval.
     */
    @Suppress("UNUSED")
    suspend fun getRoles(): Result<Set<Role>, AccessTokenExtractionException> {
        return getAuthentication().map { it.roles }
    }

    /**
     * Retrieves the groups associated with the currently authenticated user.
     *
     * @return A [Result] containing a [Set] of group keys as [String] if authentication is successful,
     * or an [AccessTokenExtractionException] if an error occurs during authentication or group retrieval.
     */
    suspend fun getGroups(): Result<Set<String>, AccessTokenExtractionException> {
        return getAuthentication().map { it.groups }
    }

    /**
     * Validates the requirement for step-up authentication and extracts a step-up token if applicable.
     *
     * @param authentication The authentication token containing information about the current session, user, and exchange.
     * @return A [Result] containing a valid [StepUpToken] if the extraction is successful, or a [TokenExtractionException] in case of failure.
     */
    suspend fun requireStepUp(authentication: AuthenticationToken): Result<StepUpToken, TokenExtractionException> {
        logger.debug { "Validating step up" }

        return stepUpTokenService.extract(
            authentication.exchange,
            authentication.userId,
            authentication.sessionId
        )
    }

    /**
     * Retrieves the current authentication from the security context and processes it to extract an
     * [AuthenticationToken]. If the authentication cannot be extracted or is of an unexpected type,
     * an appropriate error will be returned.
     *
     * @return A [Result] containing an [AuthenticationToken] if successful, or an error of type
     * [AccessTokenExtractionException] if the authentication extraction or type validation fails.
     */
    suspend fun getAuthentication(): Result<AuthenticationToken, AccessTokenExtractionException> {

        return ReactiveSecurityContextHolder.getContext()
            .awaitFirstOrNull()?.authentication
            .toResultOr { AccessTokenExtractionException.Missing("No access token found in exchange") }
            .andThen { authentication ->
                when (authentication) {
                    is AuthenticationToken -> Ok(authentication)
                    is AuthenticationFilterExceptionToken -> Err(authentication.error)
                    else -> Err(AccessTokenExtractionException.Invalid("Unexpected wrong SecurityContext: contains authentication type ${authentication::class.simpleName}"))
                }
            }
    }
}

