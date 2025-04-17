package io.stereov.web.user.model

import io.stereov.web.global.service.file.model.FileMetaData
import kotlinx.serialization.Serializable

@Serializable
data class SensitiveUserData(
    var name: String? = null,
    var email: String,
    val roles: MutableList<Role> = mutableListOf(Role.USER),
    val security: UserSecurityDetails = UserSecurityDetails(),
    val devices: MutableList<DeviceInfo> = mutableListOf(),
    var avatar: FileMetaData? = null,
)
