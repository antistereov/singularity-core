package io.stereov.singularity.content.common.content.dto

import io.stereov.singularity.content.common.content.model.ContentAccessDetails
import io.stereov.singularity.content.common.content.model.ContentAccessRole
import io.stereov.singularity.auth.model.AccessType
import io.stereov.singularity.user.model.UserDocument
import org.bson.types.ObjectId

data class ContentAccessDetailsResponse(
    val ownerId: ObjectId,
    var visibility: AccessType = AccessType.PRIVATE,
    val canEdit: Boolean,
    val canDelete: Boolean,
) {

    companion object {
        fun create(contentAccessDetails: ContentAccessDetails, user: UserDocument?): ContentAccessDetailsResponse {
            val canEdit = user?.let { contentAccessDetails.hasAccess(user, ContentAccessRole.EDITOR) } ?: false
            val canDelete = user?.let { contentAccessDetails.hasAccess(user, ContentAccessRole.ADMIN) } ?: false

            return ContentAccessDetailsResponse(
                ownerId = contentAccessDetails.ownerId,
                visibility = contentAccessDetails.visibility,
                canEdit = canEdit,
                canDelete = canDelete,
            )
        }
    }
}
