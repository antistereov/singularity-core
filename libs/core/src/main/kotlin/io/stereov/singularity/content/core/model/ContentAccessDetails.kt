package io.stereov.singularity.content.core.model

import io.stereov.singularity.auth.core.model.AuthenticationOutcome
import io.stereov.singularity.auth.token.model.AccessType
import io.stereov.singularity.content.core.dto.request.UpdateContentAccessRequest
import io.stereov.singularity.content.core.dto.response.ContentAccessDetailsResponse
import io.stereov.singularity.principal.core.model.Role
import org.bson.types.ObjectId

/**
 * Represents access control details for a specific content item, including information about
 * its visibility, users, groups, and any invitations associated with it.
 *
 * @property ownerId The unique identifier of the content owner.
 * @property visibility The visibility level of the content, which can be `PRIVATE`, `PUBLIC`, or `SHARED`.
 * By default, it is set to `PRIVATE`.
 * @property users Specifies user-level access permissions for the content.
 * @property groups Specifies group-level access permissions for the content.
 * @property invitations A set of user identifiers that have been invited to access the content.
 */
data class ContentAccessDetails(
    var ownerId: ObjectId,
    var visibility: AccessType = AccessType.PRIVATE,
    val users: ContentAccessPermissions = ContentAccessPermissions(),
    val groups: ContentAccessPermissions = ContentAccessPermissions(),
    val invitations: MutableSet<ObjectId> = mutableSetOf()
) {

    /**
     * Shares the content with the specified subject by assigning a role.
     * The content's visibility is updated to `SHARED` if it was previously `PRIVATE`.
     *
     * @param type The type of subject to share the content with, either a user or a group.
     * @param subjectId The unique identifier of the subject with whom the content is being shared.
     * @param role The access role assigned to the subject (e.g., VIEWER, EDITOR, MAINTAINER).
     * @return The updated instance of [ContentAccessDetails] after sharing the content.
     */
    fun share(type: ContentAccessSubject, subjectId: String, role: ContentAccessRole): ContentAccessDetails {
        if (visibility == AccessType.PRIVATE) visibility = AccessType.SHARED

        when (type) {
            ContentAccessSubject.USER -> users.put(subjectId, role)
            ContentAccessSubject.GROUP -> groups.put(subjectId, role)
        }

        return this
    }

    /**
     * Updates the visibility of the content to `PRIVATE` and clears all associated access configurations,
     * such as users, groups, and invitations.
     *
     * @return The updated instance of [ContentAccessDetails] after setting the content to private.
     */
    fun makePrivate(): ContentAccessDetails {
        visibility = AccessType.PRIVATE
        clear()

        return this
    }

    /**
     * Clears all user and group access configurations for the content.
     *
     * @return The updated instance of [ContentAccessDetails] after clearing all access configurations.
     */
    fun clear(): ContentAccessDetails {
        users.clear()
        groups.clear()

        return this
    }

    /**
     * Checks whether a specific subject has access to the content with a given role.
     *
     * @param type The type of the subject, which can be either `USER` or `GROUP`.
     * @param subjectId The unique identifier of the subject whose access is being verified.
     * @param role The access role to verify for the subject, such as VIEWER, EDITOR, or MAINTAINER.
     * @return `true` if the subject has access to the content with the specified role; otherwise, `false`.
     */
    fun hasAccess(type: ContentAccessSubject, subjectId: String, role: ContentAccessRole): Boolean {
        return when (type) {
            ContentAccessSubject.USER -> users.hasAccess(subjectId, role) || ObjectId(subjectId) == ownerId
            ContentAccessSubject.GROUP -> groups.hasAccess(subjectId, role)
        }
    }

    /**
     * Determines whether the given authentication outcome provides access to the content
     * with the specified role.
     *
     * @param authentication The outcome of the authentication process, which includes details
     *                        about the user's roles and group memberships.
     * @param role The content access role (e.g., `VIEWER`, `EDITOR`, `MAINTAINER`) to verify against
     *             the provided authentication outcome.
     * @return `true` if the specified authentication outcome grants the required access;
     *         otherwise, `false`.
     */
    fun hasAccess(authentication: AuthenticationOutcome, role: ContentAccessRole): Boolean {
        val isPublic = visibility == AccessType.PUBLIC

        return when (authentication) {
            is AuthenticationOutcome.None -> role == ContentAccessRole.VIEWER && isPublic
            is AuthenticationOutcome.Authenticated -> {
                val isAdmin = authentication.roles.contains(Role.User.ADMIN)
                val isOwner = authentication.principalId == ownerId

                val userIsMaintainer = hasAccess(ContentAccessSubject.USER, authentication.principalId.toString(), ContentAccessRole.MAINTAINER)
                val groupIsMaintainer = authentication.groups.any { groupId -> hasAccess(ContentAccessSubject.GROUP, groupId, ContentAccessRole.MAINTAINER) }

                val userIsEditor = hasAccess(ContentAccessSubject.USER, authentication.principalId.toString(), ContentAccessRole.EDITOR)
                val groupIsEditor = authentication.groups.any { groupId -> hasAccess(ContentAccessSubject.GROUP, groupId, ContentAccessRole.EDITOR) }

                val userIsViewer = hasAccess(ContentAccessSubject.USER, authentication.principalId.toString(), ContentAccessRole.VIEWER)
                val groupIsViewer = authentication.groups.any { groupId -> hasAccess(ContentAccessSubject.GROUP, groupId, ContentAccessRole.VIEWER) }

                when (role) {
                    ContentAccessRole.VIEWER -> isAdmin || isOwner || userIsMaintainer || groupIsMaintainer || userIsEditor || groupIsEditor || userIsViewer || groupIsViewer || isPublic
                    ContentAccessRole.EDITOR ->  isAdmin || isOwner || userIsMaintainer || groupIsMaintainer || userIsEditor || groupIsEditor
                    ContentAccessRole.MAINTAINER -> isAdmin || isOwner || userIsMaintainer || groupIsMaintainer
                }
            }
        }

    }

    private fun updateShared(
        sharedUsers: Map<ObjectId, ContentAccessRole>,
        sharedGroups: Map<String, ContentAccessRole>
    ): ContentAccessDetails {
        users.clear()
        groups.clear()

        sharedUsers.forEach { (userId, role) -> users.put(userId.toString(), role) }
        sharedGroups.forEach { (groupKey, role) -> groups.put(groupKey, role) }

        return this
    }

    fun update(req: UpdateContentAccessRequest): ContentAccessDetails {
        return when (req.accessType) {
            AccessType.PRIVATE -> makePrivate()
            AccessType.PUBLIC -> {
                visibility = AccessType.PUBLIC
                updateShared(req.sharedUsers, req.sharedGroups)

                return this
            }
            AccessType.SHARED -> {
                visibility = AccessType.SHARED
                updateShared(req.sharedUsers, req.sharedGroups)

                if (req.sharedUsers.isEmpty() && req.sharedGroups.isEmpty()) {
                    visibility = AccessType.PRIVATE
                }

                return this
            }
        }
    }

    companion object {

        fun create(req: ContentAccessDetailsResponse, ownerId: ObjectId): ContentAccessDetails {

            return when (req.visibility) {
                AccessType.PUBLIC -> {
                    val access = ContentAccessDetails(ownerId, AccessType.PUBLIC)

                    return access
                }
                AccessType.SHARED -> {
                    val access = ContentAccessDetails(ownerId, AccessType.SHARED)

                    return access
                }
                AccessType.PRIVATE -> ContentAccessDetails(ownerId, AccessType.PRIVATE)
            }
        }
    }
}
