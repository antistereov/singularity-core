package io.stereov.singularity.user.core.model

import io.stereov.singularity.auth.core.model.SessionInfo
import io.stereov.singularity.user.core.model.identity.UserIdentity
import java.util.*


data class SensitiveUserData(
    var name: String,
    var email: String?,
    val identities: MutableMap<String, UserIdentity>,
    val groups: MutableSet<String> = mutableSetOf(),
    var security: UserSecurityDetails,
    val sessions: MutableMap<UUID, SessionInfo> = mutableMapOf(),
    var avatarFileKey: String? = null,
)
