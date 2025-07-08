package io.stereov.singularity.user.model

import io.stereov.singularity.content.file.model.FileDocument


data class SensitiveUserData(
    var name: String,
    var email: String,
    val roles: MutableSet<Role> = mutableSetOf(Role.USER),
    val groups: MutableSet<String> = mutableSetOf(),
    val security: UserSecurityDetails = UserSecurityDetails(),
    val devices: MutableList<DeviceInfo> = mutableListOf(),
    var avatar: FileDocument? = null,
)
