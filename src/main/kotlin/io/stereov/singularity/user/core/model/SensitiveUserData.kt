package io.stereov.singularity.user.core.model


data class SensitiveUserData(
    var name: String,
    var email: String,
    val roles: MutableSet<Role> = mutableSetOf(Role.USER),
    val groups: MutableSet<String> = mutableSetOf(),
    val security: UserSecurityDetails,
    val sessions: MutableList<SessionInfo> = mutableListOf(),
    var avatarFileKey: String? = null,
)
