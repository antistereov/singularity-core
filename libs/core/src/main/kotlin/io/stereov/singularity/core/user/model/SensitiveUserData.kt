package io.stereov.singularity.core.user.model

import io.stereov.singularity.core.global.service.file.model.FileMetaData
import org.bson.types.ObjectId

data class SensitiveUserData(
    var name: String,
    var email: String,
    val roles: MutableSet<Role> = mutableSetOf(Role.USER),
    val groups: MutableSet<ObjectId> = mutableSetOf(),
    val security: UserSecurityDetails = UserSecurityDetails(),
    val devices: MutableList<DeviceInfo> = mutableListOf(),
    var avatar: FileMetaData? = null,
)
