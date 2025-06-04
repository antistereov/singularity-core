package io.stereov.singularity.content.common.content.model

import io.stereov.singularity.core.invitation.model.InvitationDocument
import io.stereov.singularity.core.user.model.UserDocument
import org.bson.types.ObjectId
import java.time.Instant

abstract class ContentDocument<T: ContentDocument<T>> {
    abstract val id: ObjectId
    abstract val key: String
    abstract val createdAt: Instant
    abstract var updatedAt: Instant
    abstract var access: ContentAccessDetails
    abstract val trusted: Boolean
    abstract var tags: MutableSet<String>

    @Suppress("UNCHECKED_CAST")
    fun share(type: ContentAccessSubject, subjectId: String, role: ContentAccessRole): T {
        access.share(type, subjectId, role)
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    fun addInvitation(invitation: InvitationDocument): T {
        access.invitations.add(invitation.id)

        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    fun removeInvitation(invitation: ObjectId): T {
        access.invitations.remove(invitation)

        return this as T
    }

    fun hasAccess(user: UserDocument, role: ContentAccessRole): Boolean {
        return access.hasAccess(user, role)
    }
}
