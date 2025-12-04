package io.stereov.singularity.content.core.dto.response

import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.auth.token.model.AccessType
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
        fun create(contentAccessDetails: ContentAccessDetails, authenticationOutcome: AuthenticationOutcome): ContentAccessDetailsResponse {
            val canEdit = contentAccessDetails.hasAccess(authenticationOutcome, ContentAccessRole.EDITOR)
            val canDelete =  contentAccessDetails.hasAccess(authenticationOutcome, ContentAccessRole.MAINTAINER)

            return ContentAccessDetailsResponse(
                ownerId = contentAccessDetails.ownerId,
                visibility = contentAccessDetails.visibility,
                canEdit = canEdit,
                canDelete = canDelete,
            )
        }
    }
}
