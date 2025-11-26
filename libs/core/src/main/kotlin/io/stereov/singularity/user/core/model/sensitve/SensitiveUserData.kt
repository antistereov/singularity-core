package io.stereov.singularity.user.core.model.sensitve

import io.stereov.singularity.auth.core.model.SessionInfo
import io.stereov.singularity.user.core.model.UserSecurityDetails
import io.stereov.singularity.user.core.model.identity.UserIdentities
import java.util.*

/**
 * Represents sensitive user-specific information tied to a principal.
 *
 * This data class implements [SensitivePrincipalData] and contains additional fields
 * unique to a user's account, such as email, identities, security details, and other
 * user-specific configurations. It is primarily used to manage and store critical
 * user information securely.
 *
 * @constructor Creates a new instance of [SensitiveUserData].
 *
 * @property name The name of the user. Defined as part of [SensitivePrincipalData].
 * @property email The email address of the user.
 * @property identities A collection of user identity details, including password and
 * external identity providers.
 * @property security Security-specific details, such as two-factor authentication (2FA)
 * configurations and password management information.
 * @property sessions A map associating session IDs ([UUID]) with session-specific
 * details ([SessionInfo]). Defined as part of [SensitivePrincipalData] and initialized
 * as an empty map by default.
 * @property avatarFileKey The file key associated with the user's avatar, if any.
 */
data class SensitiveUserData(
    override var name: String,
    var email: String,
    val identities: UserIdentities,
    var security: UserSecurityDetails,
    override val sessions: MutableMap<UUID, SessionInfo> = mutableMapOf(),
    var avatarFileKey: String? = null,
) : SensitivePrincipalData
