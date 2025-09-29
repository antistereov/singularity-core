package io.stereov.singularity.content.core.model

data class ContentAccessPermissions(
    val maintainer: MutableSet<String> = mutableSetOf(),
    val editor: MutableSet<String> = mutableSetOf(),
    val viewer: MutableSet<String> = mutableSetOf(),
) {

    /**
     * Remove all permissions for a given subject.
     */
    fun remove(subjectId: String): Boolean {
        val removedFromAdmins = this.maintainer.remove(subjectId)
        val removedFromEditors = this.editor.remove(subjectId)
        val removedFromViewers = this.viewer.remove(subjectId)

        return removedFromAdmins || removedFromEditors || removedFromViewers
    }

    /**
     * Add a permission for a given subject.
     */
    fun put(subjectId: String, role: ContentAccessRole) {
        remove(subjectId)

        when(role) {
            ContentAccessRole.MAINTAINER -> this.maintainer.add(subjectId)
            ContentAccessRole.EDITOR -> this.editor.add(subjectId)
            ContentAccessRole.VIEWER -> this.viewer.add(subjectId)
        }
    }

    fun isEmpty(): Boolean {
        return maintainer.isEmpty() && editor.isEmpty() && viewer.isEmpty()
    }

    fun clear() {
        this.maintainer.clear()
        this.editor.clear()
        this.viewer.clear()
    }

    fun hasAccess(subjectId: String, role: ContentAccessRole): Boolean {
        return when (role) {
            ContentAccessRole.VIEWER -> maintainer.contains(subjectId) || editor.contains(subjectId) || viewer.contains(subjectId)
            ContentAccessRole.EDITOR -> maintainer.contains(subjectId) || editor.contains(subjectId)
            ContentAccessRole.MAINTAINER -> maintainer.contains(subjectId)
        }
    }
}
