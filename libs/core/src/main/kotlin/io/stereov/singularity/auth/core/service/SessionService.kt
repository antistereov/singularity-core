package io.stereov.singularity.auth.core.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.map
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.auth.core.model.SessionInfo
import io.stereov.singularity.database.encryption.exception.SaveEncryptedDocumentException
import io.stereov.singularity.principal.core.exception.FindPrincipalByIdException
import io.stereov.singularity.principal.core.model.Principal
import io.stereov.singularity.principal.core.model.Role
import io.stereov.singularity.principal.core.model.sensitve.SensitivePrincipalData
import io.stereov.singularity.principal.core.service.PrincipalService
import org.bson.types.ObjectId
import org.springframework.stereotype.Service
import java.util.*

/**
 * Service responsible for managing user sessions, including retrieving active sessions
 * and handling session deletions for a given principal.
 * Encapsulates operations related
 * to session management and interactions with the principal service for saving updates.
 */
@Service
class SessionService(
    private val principalService: PrincipalService,
) {

    private val logger: KLogger
        get() = KotlinLogging.logger {}

    /**
     * Retrieves the sessions associated with the principal identified by the given `principalId`.
     *
     * @param principalId the unique identifier of the principal whose sessions are being requested
     * @return A [Result] containing a map of session identifiers to session information if found,
     * or an exception of type [FindPrincipalByIdException] if the principal cannot be located
     */
    suspend fun getSessions(principalId: ObjectId): Result<Map<UUID, SessionInfo>, FindPrincipalByIdException> {
        logger.debug { "Getting session" }

        return principalService.findById(principalId).map {
            it.sensitive.sessions
        }
    }

    /**
     * Deletes a specific session associated with the given principal and saves the updated principal object.
     *
     * @param principal The principal from which the session will be deleted.
     * The principal contains
     * roles, sensitive data, and session-related information.
     * @param sessionId The unique identifier of the session to be deleted.
     * @return A [Result] containing the updated [Principal] after the session is deleted, or an exception
     * [SaveEncryptedDocumentException] if there is an issue during the save operation.
     */
    suspend fun deleteSession(
        principal: Principal<out Role, out SensitivePrincipalData>,
        sessionId: UUID
    ): Result<Principal<out Role, out SensitivePrincipalData>, SaveEncryptedDocumentException> {
        logger.debug { "Deleting session $sessionId" }

        principal.removeSession(sessionId)

        return principalService.save(principal)
    }


    /**
     * Deletes all sessions associated with the given principal and updates the principal's state.
     *
     * @param principal The [Principal] whose sessions will be deleted.
     * The principal contains roles,
     * sensitive data, and session-related information.
     * @return A [Result] containing the updated [Principal] after all sessions are deleted,
     * or an exception [SaveEncryptedDocumentException] if there is an issue during the save operation.
     */
    suspend fun deleteAllSessions(
        principal: Principal<out Role, out SensitivePrincipalData>
    ): Result<Principal<out Role, out SensitivePrincipalData>, SaveEncryptedDocumentException> {
        logger.debug { "Logging out all sessions" }
        principal.clearSessions()

        return principalService.save(principal)
    }
}
