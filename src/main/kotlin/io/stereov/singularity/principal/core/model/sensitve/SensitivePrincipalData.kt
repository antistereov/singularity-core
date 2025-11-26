package io.stereov.singularity.principal.core.model.sensitve

import io.stereov.singularity.auth.core.model.SessionInfo
import java.util.*

/**
 * Represents sensitive information related to a principal, which could include user-specific
 * or guest-specific data. This interface serves as a common contract for sensitive data
 * associated with principals.
 *
 * Implementing classes are expected to hold detailed information specific to principal
 * types, such as user or guest, while adhering to the structure defined by this interface.
 *
 * Concrete implementations ara [SensitiveUserData] and [SensitiveGuestData].
 *
 * @property name The name of the principal, which may be user-modifiable or a default value.
 * @property sessions A collection of active sessions associated with the principal, using
 * UUID as the key and session-specific details encapsulated within [SessionInfo].
 */
sealed interface SensitivePrincipalData {
    val name: String
    val sessions: MutableMap<UUID, SessionInfo>
}
