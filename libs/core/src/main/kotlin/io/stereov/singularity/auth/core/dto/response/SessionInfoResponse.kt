package io.stereov.singularity.auth.core.dto.response

import io.stereov.singularity.auth.core.model.SessionInfo
import java.time.Instant
import java.util.*

data class SessionInfoResponse(
    val id: UUID,
    val browser: String? = null,
    val os: String? = null,
    val ipAddress: String?,
    val location: SessionInfo.LocationInfo?,
    val lastActive: Instant
)
