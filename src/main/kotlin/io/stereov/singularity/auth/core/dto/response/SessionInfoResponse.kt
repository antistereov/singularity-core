package io.stereov.singularity.auth.core.dto.response

import io.stereov.singularity.user.core.model.SessionInfo
import java.time.Instant

data class SessionInfoResponse(
    val id: String,
    val browser: String? = null,
    val os: String? = null,
    val ipAddress: String?,
    val location: SessionInfo.LocationInfo?,
    val lastActive: Instant
)