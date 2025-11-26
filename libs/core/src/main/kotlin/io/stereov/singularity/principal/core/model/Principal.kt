package io.stereov.singularity.principal.core.model

import com.github.michaelbull.result.Result
import io.github.oshai.kotlinlogging.KLogger
import io.stereov.singularity.auth.core.model.SessionInfo
import io.stereov.singularity.database.encryption.model.SensitiveDocument
import io.stereov.singularity.principal.core.exception.PrincipalException
import io.stereov.singularity.principal.core.model.sensitve.SensitivePrincipalData
import org.bson.types.ObjectId
import java.time.Instant
import java.util.*

/**
 * Represents a principal entity within the system, which holds roles, groups,
 * and session-related information. This sealed interface supports handling generic role types
 * and sensitive data.
 *
 * The possible principal types are [User] and [Guest].
 *
 * @param R The type of roles associated with the principal, constrained by the [Role] interface.
 * @param S The type of sensitive data associated with the principal, constrained by [SensitivePrincipalData].
 */
sealed interface Principal<R: Role, S: SensitivePrincipalData> : SensitiveDocument<S> {
    val id: Result<ObjectId, PrincipalException.InvalidDocument>
    val createdAt: Instant
    var lastActive: Instant
    val roles: Set<R>
    val groups: Set<String>
    override val sensitive: S

    val logger: KLogger

    /**
     * Update the last active timestamp of the principal.
     *
     * This method sets the last active timestamp to the current time.
     *
     * @return The updated [Principal].
     */
    fun updateLastActive(): Principal<R, S> {
        logger.debug { "Updating last active" }

        lastActive = Instant.now()
        return this
    }

    /**
     * Adds or updates a session for the current principal entity.
     * If a session with the given `sessionId` already exists, it will be removed and replaced
     * with the new `sessionInfo`. Otherwise, a new session will be added.
     *
     * @param sessionId The unique identifier of the session to be added or updated.
     * @param sessionInfo The information associated with the session, including details such as
     * refresh token, browser, OS, IP address, and location.
     * @return The updated principal entity after the session is added or updated.
     */
    fun addOrUpdateSession(sessionId: UUID, sessionInfo: SessionInfo): Principal<R, S> {
        logger.debug { "Adding or updating session $sessionId" }

        removeSession(sessionId)
        this.sensitive.sessions[sessionId] = sessionInfo

        return this
    }

    /**
     * Removes a session for the current principal entity based on the provided session identifier.
     *
     * @param sessionId The unique identifier of the session to be removed.
     * @return The updated principal entity after the session is removed.
     */
    fun removeSession(sessionId: UUID): Principal<R, S> {
        logger.debug { "Removing session $sessionId" }

        this.sensitive.sessions.remove(sessionId)

        return this
    }

    /**
     * Clears all active sessions associated with the current principal entity.
     *
     * This method removes all session data stored in the `sensitive.sessions` field,
     * effectively logging out the principal from all active sessions. The operation
     * does not impact other principal-related data or attributes.
     *
     * @return The updated principal entity after all sessions have been cleared.
     */
    fun clearSessions(): Principal<R, S> {
        logger.debug { "Clearing sessions" }

        sensitive.sessions.clear()

        return this
    }
}
