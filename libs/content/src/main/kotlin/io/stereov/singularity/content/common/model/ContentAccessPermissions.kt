package io.stereov.singularity.content.common.model

import org.bson.types.ObjectId

data class ContentAccessPermissions(
    val admin: MutableSet<ObjectId> = mutableSetOf(),
    val editor: MutableSet<ObjectId> = mutableSetOf(),
    val viewer: MutableSet<ObjectId> = mutableSetOf(),
) {

    /**
     * Remove all permissions for a given subject.
     */
    fun remove(subjectId: ObjectId): Boolean {
        val removedFromAdmins = this.admin.remove(subjectId)
        val removedFromEditors = this.editor.remove(subjectId)
        val removedFromViewers = this.viewer.remove(subjectId)

        return removedFromAdmins || removedFromEditors || removedFromViewers
    }

    /**
     * Add a permission for a given subject.
     */
    fun put(subjectId: ObjectId, role: ContentAccessRole) {
        remove(subjectId)

        when(role) {
            ContentAccessRole.ADMIN -> this.admin.add(subjectId)
            ContentAccessRole.EDITOR -> this.editor.add(subjectId)
            ContentAccessRole.VIEWER -> this.viewer.add(subjectId)
        }
    }

    fun isEmpty(): Boolean {
        return admin.isEmpty() && editor.isEmpty() && viewer.isEmpty()
    }

    fun clear() {
        this.admin.clear()
        this.editor.clear()
        this.viewer.clear()
    }

    fun hasAccess(subjectId: ObjectId, role: ContentAccessRole): Boolean {
        return when (role) {
            ContentAccessRole.VIEWER -> admin.contains(subjectId) || editor.contains(subjectId) || viewer.contains(subjectId)
            ContentAccessRole.EDITOR -> admin.contains(subjectId) || editor.contains(subjectId)
            ContentAccessRole.ADMIN -> admin.contains(subjectId)
        }
    }
}
