package io.stereov.singularity.content.common.content.dto

import io.stereov.singularity.content.common.content.model.ContentAccessDetails
import io.stereov.singularity.content.common.content.model.ContentAccessRole
import io.stereov.singularity.auth.model.AccessType
import io.stereov.singularity.invitation.dto.InvitationResponse
import io.stereov.singularity.invitation.model.InvitationDocument
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
            contentAccessDetails.groups.viewer.forEach { group -> groups.put(group, ContentAccessRole.VIEWER) }
            contentAccessDetails.groups.editor.forEach { group -> groups.put(group, ContentAccessRole.EDITOR) }
            contentAccessDetails.groups.admin.forEach { group -> groups.put(group, ContentAccessRole.ADMIN) }

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
