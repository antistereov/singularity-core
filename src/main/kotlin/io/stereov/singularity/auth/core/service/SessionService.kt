package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.user.core.model.SessionInfo
import io.stereov.singularity.user.core.model.UserDocument
import io.stereov.singularity.user.core.service.UserService
import org.springframework.stereotype.Service

@Service
class SessionService(
    private val userService: UserService,
    private val authorizationService: AuthorizationService,
    private val accessTokenCache: AccessTokenCache,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Gets the list of sessions associated with the current user.
     *
     * @return A list of [SessionInfo] objects representing the user's sessions.
     */
    suspend fun getSessions(): List<SessionInfo> {
        logger.debug { "Getting session" }

        return authorizationService.getCurrentUser().sensitive.sessions
    }

    /**
     * Removes a session from the current user's account.
     *
     * @param sessionId The ID of the session to remove.
     *
     * @return The updated [UserDocument] of the user.
     */
    suspend fun deleteSession(sessionId: String): UserDocument {
        logger.debug { "Deleting session $sessionId" }

        val user = authorizationService.getCurrentUser()

        user.removeSession(sessionId)

        return userService.save(user)
    }


    /**
     * Logs out the user from all sessions and returns the updated user document.
     *
     * @return The [UserDocument] of the logged-out user.
     */
    suspend fun deleteAllSessions(): UserDocument {
        logger.debug { "Logging out all sessions" }

        val user = authorizationService.getCurrentUser()
        accessTokenCache.invalidateAllTokens(user.id)
        user.clearSessions()

        return userService.save(user)
    }
}