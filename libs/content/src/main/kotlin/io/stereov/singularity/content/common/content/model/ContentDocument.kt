package io.stereov.singularity.content.common.content.model

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
}
