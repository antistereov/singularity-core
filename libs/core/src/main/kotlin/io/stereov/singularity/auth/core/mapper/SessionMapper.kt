package io.stereov.singularity.auth.core.mapper

import io.stereov.singularity.auth.core.dto.response.SessionInfoResponse
import io.stereov.singularity.auth.core.model.SessionInfo
import org.springframework.stereotype.Component
import java.util.*

@Component
class SessionMapper {

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
