package io.stereov.singularity.auth.core.mapper

import io.stereov.singularity.auth.core.dto.response.SessionInfoResponse
import io.stereov.singularity.auth.core.model.SessionInfo
import org.springframework.stereotype.Component
import java.util.*

/**
 * A utility component responsible for transforming session-related data into response objects
 * used by various components or services within the application.
 */
@Component
class SessionMapper {

    /**
     * Converts a map of session details into a list of session response objects.
     *
     * @param sessions A map where the key is the session's unique identifier (UUID) and the value is
     *                 the session's information encapsulated in a [SessionInfo] object.
     * @return A list of [SessionInfoResponse] objects containing transformed session information.
     */
    fun toSessionInfoResponse(sessions: Map<UUID, SessionInfo>): List<SessionInfoResponse> {
        return sessions.map { (id, info) ->
            SessionInfoResponse(
                id,
                info.browser,
                info.os,
                info.ipAddress,
                info.location,
                info.issuedAt
            )
        }
    }
}
