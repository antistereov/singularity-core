package io.stereov.singularity.global.service.file.model

import io.stereov.singularity.auth.model.AccessType
import io.stereov.singularity.auth.model.Permission
import io.stereov.singularity.auth.model.SharedPermission
import io.stereov.singularity.global.serializer.InstantSerializer
import kotlinx.serialization.Serializable
import java.time.Instant

@Serializable
data class FileMetaData(
    val key: String,
    val owner: String,
    val contentType: String,
    val accessType: AccessType,
    val sharedWith: List<SharedPermission>,
    val publicUrl: String?,
    val size: Long,
    @Serializable(with = InstantSerializer::class)
    val uploaded: Instant = Instant.now()
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
