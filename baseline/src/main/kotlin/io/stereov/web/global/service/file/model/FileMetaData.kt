package io.stereov.web.global.service.file.model

import io.stereov.web.auth.model.AccessType
import io.stereov.web.auth.model.Permission
import io.stereov.web.auth.model.SharedPermission
import kotlinx.serialization.Serializable

@Serializable
data class FileMetaData(
    val key: String,
    val owner: String,
    val contentType: String,
    val accessType: AccessType,
    val sharedWith: List<SharedPermission>,
    val publicUrl: String?,
    val size: Long,
) {

    fun hasPermission(userId: String, permission: Permission): Boolean {
        if (this.owner == userId) return true

        if (this.accessType == AccessType.PUBLIC && permission == Permission.READ) return true

        return this.sharedWith.any {
            it.userId == userId && permission in it.permissions
        }
    }

    fun getPermissions(userId: String): Set<Permission> {
        val permissions = mutableSetOf<Permission>()

        if (hasPermission(userId, Permission.READ)) permissions.add(Permission.READ)
        if (hasPermission(userId, Permission.WRITE)) permissions.add(Permission.WRITE)

        return permissions
    }
}
