package io.stereov.singularity.content.core.model

import io.stereov.singularity.auth.core.model.token.AccessType
import io.stereov.singularity.auth.core.model.token.CustomAuthenticationToken
import io.stereov.singularity.content.core.dto.request.UpdateContentAccessRequest
import io.stereov.singularity.content.core.dto.response.ContentAccessDetailsResponse
import io.stereov.singularity.user.core.model.Role
import org.bson.types.ObjectId

data class ContentAccessDetails(
    var ownerId: ObjectId,
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

    fun hasAccess(authentication: CustomAuthenticationToken, role: ContentAccessRole): Boolean {
        val isAdmin = authentication.roles.contains(Role.ADMIN)
        val isOwner = authentication.userId == ownerId

        val userIsMaintainer = hasAccess(ContentAccessSubject.USER, authentication.userId.toString(), ContentAccessRole.MAINTAINER)
        val groupIsMaintainer = authentication.groups.any { groupId -> hasAccess(ContentAccessSubject.GROUP, groupId, ContentAccessRole.MAINTAINER) }

        val userIsEditor = hasAccess(ContentAccessSubject.USER, authentication.userId.toString(), ContentAccessRole.EDITOR)
        val groupIsEditor = authentication.groups.any { groupId -> hasAccess(ContentAccessSubject.GROUP, groupId, ContentAccessRole.EDITOR) }

        val userIsViewer = hasAccess(ContentAccessSubject.USER, authentication.userId.toString(), ContentAccessRole.VIEWER)
        val groupIsViewer = authentication.groups.any { groupId -> hasAccess(ContentAccessSubject.GROUP, groupId, ContentAccessRole.VIEWER) }

        val isPublic = visibility == AccessType.PUBLIC

        return when (role) {
            ContentAccessRole.VIEWER -> isAdmin || isOwner || userIsMaintainer || groupIsMaintainer || userIsEditor || groupIsEditor || userIsViewer || groupIsViewer || isPublic
            ContentAccessRole.EDITOR ->  isAdmin || isOwner || userIsMaintainer || groupIsMaintainer || userIsEditor || groupIsEditor
            ContentAccessRole.MAINTAINER -> isAdmin || isOwner || userIsMaintainer || groupIsMaintainer
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

    fun update(req: UpdateContentAccessRequest): ContentAccessDetails {
        return when (req.accessType) {
            AccessType.PRIVATE -> makePrivate()
            AccessType.PUBLIC -> {
                visibility = AccessType.PUBLIC
                updateShared(req.sharedUsers, req.sharedGroups)

                return this
            }
            AccessType.SHARED -> {
                visibility = AccessType.SHARED
                updateShared(req.sharedUsers, req.sharedGroups)

                if (req.sharedUsers.isEmpty() && req.sharedGroups.isEmpty()) {
                    visibility = AccessType.PRIVATE
                }

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
