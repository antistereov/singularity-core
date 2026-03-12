package io.stereov.singularity.content.core.model

import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.auth.token.model.AccessType
import io.stereov.singularity.database.core.model.DocumentKey
import io.stereov.singularity.database.core.model.WithKey
import org.bson.types.ObjectId
import org.springframework.data.annotation.Transient
import java.time.Instant

/**
 * Represents a document that can be stored within a content management system. This interface serves
 * as a base for creating content-specific implementations.
 *
 * @param D the type of the implementing class, ensuring type-safe chaining when using methods that return the instance.
 *
 * @property _id The unique identifier of the document.
 * @property key The unique key used to identify the document within the system.
 * @property createdAt The date and time when the document was created.
 * @property updatedAt The date and time when the document was last updated.
 * @property access The access control details for the content.
 * @property trusted Indicates whether the system trusts the content.
 * @property tags A set of tags associated with the content.
 */
interface ContentDocument<D: ContentDocument<D>> : WithKey {
    override val _id: ObjectId?
    override val key: DocumentKey
    val createdAt: Instant
    var updatedAt: Instant
    var access: ContentAccessDetails
    var trusted: Boolean
    var tags: MutableSet<DocumentKey>

    /**
     * Indicates whether the content is publicly accessible.
     *
     * This property evaluates the visibility of the content's access settings.
     * It returns `true` if the content's access type is set to `PUBLIC`,
     * meaning the content can be accessed by anyone without specific restrictions.
     * For other access types (e.g., `PRIVATE` or `SHARED`), it returns `false`.
     */
    @get:Transient
    val isPublic: Boolean
        get() = access.visibility == AccessType.PUBLIC

    /**
     * Shares the content with the specified subject and assigns a role to them.
     * This allows the subject to access or interact with the content based on the assigned role.
     *
     * @param subject The unique identifier of the subject to whom the content is being shared.
     * @param role The access role to assign to the subject (e.g., VIEWER, EDITOR, MAINTAINER).
     * @return The instance of the implementing class for method chaining.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T: ContentAccessSubject> share(subject: T, role: ContentAccessRole): D {
        access.share(subject, role)
        return this as D
    }

    @Suppress("UNCHECKED_CAST")
    fun addInvitation(invitationId: ObjectId): D {
        access.invitations.add(invitationId)

        return this as D
    }

    @Suppress("UNCHECKED_CAST")
    fun removeInvitation(invitation: ObjectId): D {
        access.invitations.remove(invitation)

        return this as D
    }

    /**
     * Checks whether the given authentication outcome has access to the content with the specified role.
     *
     * @param authentication The authentication outcome representing the authenticated user or session.
     * @param role The content access role to be checked (e.g., VIEWER, EDITOR, MAINTAINER).
     * @return `true` if the authentication has access to the content with the specified role, otherwise `false`.
     */
    fun hasAccess(authentication: AuthenticationOutcome, role: ContentAccessRole): Boolean {
        return access.hasAccess(authentication, role)
    }
}
