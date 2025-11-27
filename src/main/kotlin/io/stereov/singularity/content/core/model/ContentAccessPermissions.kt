package io.stereov.singularity.content.core.model

/**
 * Represents a set of permissions for accessing content, categorized into three roles:
 * maintainer, editor, and viewer. Each role is associated with a mutable set of subject IDs,
 * allowing dynamic modification of permissions.
 *
 * @property maintainer A mutable set containing IDs of subjects with "maintainer" permissions.
 * @property editor A mutable set containing IDs of subjects with "editor" permissions.
 * @property viewer A mutable set containing IDs of subjects with "viewer" permissions.
 */
data class ContentAccessPermissions(
    val maintainer: MutableSet<String> = mutableSetOf(),
    val editor: MutableSet<String> = mutableSetOf(),
    val viewer: MutableSet<String> = mutableSetOf(),
) {

    /**
     * Removes a subject's permissions from all roles, including maintainer, editor, and viewer.
     *
     * @param subjectId The unique identifier of the subject whose permissions are to be removed.
     * @return `true` if the subject was removed from at least one role, `false` if the subject did not exist in any role.
     */
    fun remove(subjectId: String): Boolean {
        val removedFromAdmins = this.maintainer.remove(subjectId)
        val removedFromEditors = this.editor.remove(subjectId)
        val removedFromViewers = this.viewer.remove(subjectId)

        return removedFromAdmins || removedFromEditors || removedFromViewers
    }

    /**
     * Assigns a specific role to a subject by first removing any existing permissions for the subject
     * and then adding the subject to the list associated with the specified role.
     *
     * @param subjectId The unique identifier of the subject to which the role is assigned.
     * @param role The role to be assigned to the subject, such as MAINTAINER, EDITOR, or VIEWER.
     */
    fun put(subjectId: String, role: ContentAccessRole) {
        remove(subjectId)

        when(role) {
            ContentAccessRole.MAINTAINER -> this.maintainer.add(subjectId)
            ContentAccessRole.EDITOR -> this.editor.add(subjectId)
            ContentAccessRole.VIEWER -> this.viewer.add(subjectId)
        }
    }

    /**
     * Checks whether all permissions (maintainer, editor, and viewer) are empty.
     *
     * @return `true` if no permissions are set for any role, `false` otherwise.
     */
    fun isEmpty(): Boolean {
        return maintainer.isEmpty() && editor.isEmpty() && viewer.isEmpty()
    }

    /**
     * Clears all access permissions associated with the maintainer, editor, and viewer roles.
     *
     * This method removes any assignments or associations present in the corresponding
     * lists for these roles, resetting them to an empty state.
     */
    fun clear() {
        this.maintainer.clear()
        this.editor.clear()
        this.viewer.clear()
    }

    /**
     * Determines whether a subject ID has access to a given role based on the permissions assigned.
     *
     * @param subjectId The identifier of the subject whose access is to be checked.
     * @param role The role for which access needs to be verified, such as VIEWER, EDITOR, or MAINTAINER.
     * @return `true` if the subject has access to the specified role, `false` otherwise.
     */
    fun hasAccess(subjectId: String, role: ContentAccessRole): Boolean {
        return when (role) {
            ContentAccessRole.VIEWER -> maintainer.contains(subjectId) || editor.contains(subjectId) || viewer.contains(subjectId)
            ContentAccessRole.EDITOR -> maintainer.contains(subjectId) || editor.contains(subjectId)
            ContentAccessRole.MAINTAINER -> maintainer.contains(subjectId)
        }
    }
}
