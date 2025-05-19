package io.stereov.singularity.content.common.content.dto

import io.stereov.singularity.content.common.content.model.ContentAccessDetails
import io.stereov.singularity.content.common.content.model.ContentAccessRole
import io.stereov.singularity.core.auth.model.AccessType
import io.stereov.singularity.core.user.model.UserDocument
import org.bson.types.ObjectId

data class ContentAccessDetailsResponse(
    val ownerId: ObjectId,
    var visibility: AccessType = AccessType.PRIVATE,
    val users: Map<ObjectId, ContentAccessRole>,
    val groups: Map<String, ContentAccessRole>,
    val canEdit: Boolean,
    val canDelete: Boolean
) {

    companion object {
        fun create(contentAccessDetails: ContentAccessDetails, user: UserDocument?): ContentAccessDetailsResponse {
            val canEdit = user?.let { contentAccessDetails.hasAccess(user, ContentAccessRole.EDITOR) } ?: false
            val canDelete = user?.let { contentAccessDetails.hasAccess(user, ContentAccessRole.ADMIN) } ?: false

            val users = mutableMapOf<ObjectId, ContentAccessRole>()
            contentAccessDetails.users.viewer.forEach { user -> users.put(ObjectId(user), ContentAccessRole.VIEWER) }
            contentAccessDetails.users.editor.forEach { user -> users.put(ObjectId(user), ContentAccessRole.EDITOR) }
            contentAccessDetails.users.admin.forEach { user -> users.put(ObjectId(user), ContentAccessRole.ADMIN) }

            val groups = mutableMapOf<String, ContentAccessRole>()
            contentAccessDetails.groups.viewer.forEach { group -> groups.put(group, ContentAccessRole.VIEWER) }
            contentAccessDetails.groups.editor.forEach { group -> groups.put(group, ContentAccessRole.EDITOR) }
            contentAccessDetails.groups.admin.forEach { group -> groups.put(group, ContentAccessRole.ADMIN) }

            return ContentAccessDetailsResponse(
                ownerId = contentAccessDetails.ownerId,
                visibility = contentAccessDetails.visibility,
                users = users,
                groups = groups,
                canEdit = canEdit,
                canDelete = canDelete
            )
        }
    }
}
