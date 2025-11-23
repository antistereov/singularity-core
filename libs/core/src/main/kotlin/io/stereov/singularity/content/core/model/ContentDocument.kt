package io.stereov.singularity.content.core.model

import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.auth.core.model.token.AccessType
import io.stereov.singularity.content.invitation.model.InvitationDocument
import org.bson.types.ObjectId
import org.springframework.data.annotation.Transient
import java.time.Instant

interface ContentDocument<T: ContentDocument<T>> {
    val id: ObjectId
    val key: String
    val createdAt: Instant
    var updatedAt: Instant
    var access: ContentAccessDetails
    var trusted: Boolean
    var tags: MutableSet<String>

    @get:Transient
    val isPublic: Boolean
        get() = access.visibility == AccessType.PUBLIC


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

    fun hasAccess(authentication: AuthenticationOutcome, role: ContentAccessRole): Boolean {
        return access.hasAccess(authentication, role)
    }
}
