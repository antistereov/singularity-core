package io.stereov.singularity.content.common.content.model

import io.stereov.singularity.content.common.content.dto.ContentAccessDetailsResponse
import io.stereov.singularity.core.auth.model.AccessType
import io.stereov.singularity.core.user.model.UserDocument
import org.bson.types.ObjectId

data class ContentAccessDetails(
    val ownerId: ObjectId,
    var visibility: AccessType = AccessType.PRIVATE,
    val users: ContentAccessPermissions = ContentAccessPermissions(),
    val groups: ContentAccessPermissions = ContentAccessPermissions(),
) {

    constructor(response: ContentAccessDetailsResponse): this(
        response.ownerId,
        response.visibility,
        response.users,
        response.groups
    )

    fun share(type: ContentAccessSubject, subjectId: ObjectId, role: ContentAccessRole): ContentAccessDetails {
        if (visibility == AccessType.PRIVATE) visibility = AccessType.SHARED

        when (type) {
            ContentAccessSubject.USER -> users.put(subjectId, role)
            ContentAccessSubject.GROUP -> groups.put(subjectId, role)
        }

        return this
    }

    fun remove(type: ContentAccessSubject, subjectId: ObjectId): ContentAccessDetails {
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

    fun hasAccess(type: ContentAccessSubject, subjectId: ObjectId, role: ContentAccessRole): Boolean {
        return when (type) {
            ContentAccessSubject.USER -> users.hasAccess(subjectId, role) || subjectId == ownerId
            ContentAccessSubject.GROUP -> groups.hasAccess(subjectId, role)
        }
    }

    fun hasAccess(user: UserDocument, role: ContentAccessRole): Boolean {
        val userIsAdmin = hasAccess(ContentAccessSubject.USER, user.id, ContentAccessRole.ADMIN)
        val groupIsAdmin = user.sensitive.groups.any { groupId -> hasAccess(ContentAccessSubject.GROUP, groupId, ContentAccessRole.ADMIN) }

        val userIsEditor = hasAccess(ContentAccessSubject.USER, user.id, ContentAccessRole.EDITOR)
        val groupIsEditor = user.sensitive.groups.any { groupId -> hasAccess(ContentAccessSubject.GROUP, groupId, ContentAccessRole.EDITOR) }

        val userIsViewer = hasAccess(ContentAccessSubject.USER, user.id, ContentAccessRole.VIEWER)
        val groupIsViewer = user.sensitive.groups.any { groupId -> hasAccess(ContentAccessSubject.GROUP, groupId, ContentAccessRole.VIEWER) }

        return when (role) {
            ContentAccessRole.VIEWER ->  userIsAdmin || groupIsAdmin || userIsEditor || groupIsEditor || userIsViewer || groupIsViewer
            ContentAccessRole.EDITOR ->  userIsAdmin || groupIsAdmin || userIsEditor || groupIsEditor
            ContentAccessRole.ADMIN -> userIsAdmin || groupIsAdmin
        }
    }
}
