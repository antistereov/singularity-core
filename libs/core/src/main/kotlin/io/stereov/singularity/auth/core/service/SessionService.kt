package io.stereov.singularity.auth.core.service

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.model.SessionInfo
import io.stereov.singularity.principal.core.model.User
import io.stereov.singularity.principal.core.service.UserService
import org.springframework.stereotype.Service
import java.util.*

@Service
class SessionService(
    private val userService: UserService,
    private val authorizationService: AuthorizationService,
    private val accessTokenCache: AccessTokenCache,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    suspend fun getSessions(): Map<UUID, SessionInfo> {
        logger.debug { "Getting session" }

        return authorizationService.getUser().sensitive.sessions
    }

    /**
     * Removes a session from the current user's account.
     *
     * @param sessionId The ID of the session to remove.
     *
     * @return The updated [User] of the user.
     */
    suspend fun deleteSession(sessionId: UUID): User {
        logger.debug { "Deleting session $sessionId" }

        val user = authorizationService.getUser()
        accessTokenCache.invalidateSessionTokens(user.id, sessionId)

        user.removeSession(sessionId)

        return userService.save(user)
    }


    /**
     * Logs out the user from all sessions and returns the updated user document.
     *
     * @return The [User] of the logged-out user.
     */
    suspend fun deleteAllSessions(): User {
        logger.debug { "Logging out all sessions" }

        val user = authorizationService.getUser()
        accessTokenCache.invalidateAllTokens(user.id)
        user.clearSessions()

        return userService.save(user)
    }
}
