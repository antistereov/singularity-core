package io.stereov.singularity.user.core.model.sensitve

import io.stereov.singularity.auth.core.model.SessionInfo
import java.util.*

/**
 * Represents the sensitive data associated with a guest in the system.
 *
 * This data class provides the structure for handling sensitive information
 * specific to guest entities, such as their name and active session details.
 * It implements [SensitivePrincipalData], which serves as the base interface
 * for sensitive principal data across different types of principals.
 *
 * @property name The name of the guest. By default, it is set to "Guest".
 * @property sessions A mutable map containing the guest's active sessions, where
 * each session is identified by a UUID and contains session-specific details
 * encapsulated within [SessionInfo].
 */
data class SensitiveGuestData(
    override var name: String = "Guest",
    override val sessions: MutableMap<UUID, SessionInfo> = mutableMapOf()
) : SensitivePrincipalData
