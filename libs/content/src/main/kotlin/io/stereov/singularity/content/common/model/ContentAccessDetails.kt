package io.stereov.singularity.content.common.model

import io.stereov.singularity.core.auth.model.AccessType
import kotlinx.serialization.Serializable

@Serializable
data class ContentAccessDetails(
    val ownerId: String,
    var visibility: AccessType = AccessType.PRIVATE,
    val users: ContentAccessPermissions = ContentAccessPermissions(),
    val groups: ContentAccessPermissions = ContentAccessPermissions(),
) {

    fun share(type: ContentAccessSubject, subjectId: String, role: ContentAccessRole): ContentAccessDetails {
        if (visibility == AccessType.PRIVATE) visibility = AccessType.SHARED

        when (type) {
            ContentAccessSubject.USER -> users.put(subjectId, role)
            ContentAccessSubject.GROUP -> groups.put(subjectId, role)
        }

        return this
    }

    fun remove(type: ContentAccessSubject, subjectId: String): ContentAccessDetails {
        when (type) {
            ContentAccessSubject.USER -> users.remove(subjectId)
            ContentAccessSubject.GROUP -> groups.remove(subjectId)
        }

        if (users.isEmpty() && groups.isEmpty() && visibility == AccessType.SHARED) visibility = AccessType.PRIVATE

        return this
    }

    fun publish(): ContentAccessDetails {
        visibility = AccessType.PUBLIC

        return this
    }

    fun makePrivate(): ContentAccessDetails {
        visibility = AccessType.PRIVATE
        users.clear()
        groups.clear()

        return this
    }

    fun hasAccess(type: ContentAccessSubject, subjectId: String, role: ContentAccessRole): Boolean {
        return when (type) {
            ContentAccessSubject.USER -> users.hasAccess(subjectId, role)
            ContentAccessSubject.GROUP -> groups.hasAccess(subjectId, role)
        }
    }
}
