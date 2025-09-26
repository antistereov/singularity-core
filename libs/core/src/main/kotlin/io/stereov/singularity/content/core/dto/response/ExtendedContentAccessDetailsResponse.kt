package io.stereov.singularity.content.core.dto.response

import io.stereov.singularity.auth.core.model.token.AccessType
import io.stereov.singularity.content.core.model.ContentAccessDetails
import io.stereov.singularity.content.core.model.ContentAccessRole
import io.stereov.singularity.content.invitation.dto.InvitationResponse
import io.stereov.singularity.content.invitation.model.InvitationDocument
import org.bson.types.ObjectId

data class ExtendedContentAccessDetailsResponse(
    val ownerId: ObjectId,
    var visibility: AccessType = AccessType.PRIVATE,
    val users: List<UserContentAccessDetails>,
    val groups: Map<String, ContentAccessRole>,
    val invitations: List<InvitationResponse>
) {

    companion object {
        fun create(contentAccessDetails: ContentAccessDetails, invitations: List<InvitationDocument>, users: List<UserContentAccessDetails>): ExtendedContentAccessDetailsResponse {
            val groups = mutableMapOf<String, ContentAccessRole>()
            contentAccessDetails.groups.viewer.forEach { group -> groups[group] = ContentAccessRole.VIEWER }
            contentAccessDetails.groups.editor.forEach { group -> groups[group] = ContentAccessRole.EDITOR }
            contentAccessDetails.groups.maintainer.forEach { group -> groups[group] = ContentAccessRole.MAINTAINER }

            return ExtendedContentAccessDetailsResponse(
                ownerId = contentAccessDetails.ownerId,
                visibility = contentAccessDetails.visibility,
                users = users,
                groups = groups,
                invitations = invitations.map { it.toInvitationResponse() }
            )
        }
    }
}
