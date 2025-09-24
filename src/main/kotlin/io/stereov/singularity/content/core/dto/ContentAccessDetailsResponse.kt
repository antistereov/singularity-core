package io.stereov.singularity.content.core.dto

import io.stereov.singularity.auth.core.model.token.AccessType
import io.stereov.singularity.auth.core.model.token.CustomAuthenticationToken
import io.stereov.singularity.content.core.model.ContentAccessDetails
import io.stereov.singularity.content.core.model.ContentAccessRole
import org.bson.types.ObjectId

data class ContentAccessDetailsResponse(
    val ownerId: ObjectId,
    var visibility: AccessType = AccessType.PRIVATE,
    val canEdit: Boolean,
    val canDelete: Boolean,
) {

    companion object {
        fun create(contentAccessDetails: ContentAccessDetails, authentication: CustomAuthenticationToken?): ContentAccessDetailsResponse {
            val canEdit = authentication?.let { contentAccessDetails.hasAccess(authentication, ContentAccessRole.EDITOR) } ?: false
            val canDelete = authentication?.let { contentAccessDetails.hasAccess(authentication, ContentAccessRole.ADMIN) } ?: false

            return ContentAccessDetailsResponse(
                ownerId = contentAccessDetails.ownerId,
                visibility = contentAccessDetails.visibility,
                canEdit = canEdit,
                canDelete = canDelete,
            )
        }
    }
}
