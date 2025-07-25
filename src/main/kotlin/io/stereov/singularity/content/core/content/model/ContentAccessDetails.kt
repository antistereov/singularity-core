package io.stereov.singularity.content.common.content.model

import io.stereov.singularity.auth.model.AccessType
import io.stereov.singularity.content.common.content.dto.ChangeContentVisibilityRequest
import io.stereov.singularity.content.common.content.dto.ContentAccessDetailsResponse
import io.stereov.singularity.user.model.Role
import io.stereov.singularity.user.model.UserDocument
import org.bson.types.ObjectId

data class ContentAccessDetails(
    val ownerId: ObjectId,
    var visibility: AccessType = AccessType.PRIVATE,
    val users: ContentAccessPermissions = ContentAccessPermissions(),
    val groups: ContentAccessPermissions = ContentAccessPermissions(),
    val invitations: MutableSet<ObjectId> = mutableSetOf()
) {

    fun share(type: ContentAccessSubject, subjectId: String, role: ContentAccessRole): ContentAccessDetails {
        if (visibility == AccessType.PRIVATE) visibility = AccessType.SHARED

        when (type) {
            ContentAccessSubject.USER -> users.put(subjectId, role)
            ContentAccessSubject.GROUP -> groups.put(subjectId, role)
        }

        return this
    }

    fun makePrivate(): ContentAccessDetails {
        visibility = AccessType.PRIVATE
        clear()

        return this
    }

    fun clear(): ContentAccessDetails {
        users.clear()
        groups.clear()

        return this
    }

    fun hasAccess(type: ContentAccessSubject, subjectId: String, role: ContentAccessRole): Boolean {
        return when (type) {
            ContentAccessSubject.USER -> users.hasAccess(subjectId, role) || ObjectId(subjectId) == ownerId
            ContentAccessSubject.GROUP -> groups.hasAccess(subjectId, role)
        }
    }

    fun hasAccess(user: UserDocument, role: ContentAccessRole): Boolean {
        val isAdmin = user.sensitive.roles.contains(Role.ADMIN)

        val userIsAdmin = hasAccess(ContentAccessSubject.USER, user.id.toString(), ContentAccessRole.ADMIN)
        val groupIsAdmin = user.sensitive.groups.any { groupId -> hasAccess(ContentAccessSubject.GROUP, groupId, ContentAccessRole.ADMIN) }

        val userIsEditor = hasAccess(ContentAccessSubject.USER, user.id.toString(), ContentAccessRole.EDITOR)
        val groupIsEditor = user.sensitive.groups.any { groupId -> hasAccess(ContentAccessSubject.GROUP, groupId, ContentAccessRole.EDITOR) }

        val userIsViewer = hasAccess(ContentAccessSubject.USER, user.id.toString(), ContentAccessRole.VIEWER)
        val groupIsViewer = user.sensitive.groups.any { groupId -> hasAccess(ContentAccessSubject.GROUP, groupId, ContentAccessRole.VIEWER) }

        val isPublic = visibility == AccessType.PUBLIC

        return when (role) {
            ContentAccessRole.VIEWER -> isAdmin || userIsAdmin || groupIsAdmin || userIsEditor || groupIsEditor || userIsViewer || groupIsViewer || isPublic
            ContentAccessRole.EDITOR ->  isAdmin || userIsAdmin || groupIsAdmin || userIsEditor || groupIsEditor
            ContentAccessRole.ADMIN -> isAdmin || userIsAdmin || groupIsAdmin
        }
    }

    private fun updateShared(
        sharedUsers: Map<ObjectId, ContentAccessRole>,
        sharedGroups: Map<String, ContentAccessRole>
    ): ContentAccessDetails {
        users.clear()
        groups.clear()

        sharedUsers.forEach { (userId, role) -> users.put(userId.toString(), role) }
        sharedGroups.forEach { (groupKey, role) -> groups.put(groupKey, role) }

        return this
    }

    fun update(req: ChangeContentVisibilityRequest): ContentAccessDetails {
        return when (req.visibility) {
            AccessType.PRIVATE -> makePrivate()
            AccessType.PUBLIC -> {
                visibility = AccessType.PUBLIC
                updateShared(req.sharedUsers, req.sharedGroups)

                return this
            }
            AccessType.SHARED -> {
                visibility = AccessType.SHARED
                updateShared(req.sharedUsers, req.sharedGroups)

                return this
            }
        }
    }

    companion object {

        fun create(req: ContentAccessDetailsResponse, ownerId: ObjectId): ContentAccessDetails {

            return when (req.visibility) {
                AccessType.PUBLIC -> {
                    val access = ContentAccessDetails(ownerId, AccessType.PUBLIC)

                    return access
                }
                AccessType.SHARED -> {
                    val access = ContentAccessDetails(ownerId, AccessType.SHARED)

                    return access
                }
                AccessType.PRIVATE -> ContentAccessDetails(ownerId, AccessType.PRIVATE)
            }
        }
    }
}
