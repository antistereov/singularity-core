package io.stereov.singularity.content.common.content.model

import org.bson.types.ObjectId
import java.time.Instant

abstract class ContentDocument<T: ContentDocument<T>> {
    abstract val id: ObjectId
    abstract val key: String
    abstract val createdAt: Instant
    abstract var updatedAt: Instant
    abstract val access: ContentAccessDetails
    abstract val trusted: Boolean
    abstract val tags: MutableSet<String>

    @Suppress("UNCHECKED_CAST")
    fun share(type: ContentAccessSubject, subjectId: String, role: ContentAccessRole): T {
        access.share(type, subjectId, role)
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    fun remove(type: ContentAccessSubject, subjectId: String): T {
        access.remove(type, subjectId)
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    fun publish(): T {
        access.publish()
        return this as T
    }

    @Suppress("UNCHECKED_CAST")
    fun makePrivate(): T {
        access.makePrivate()
        return this as T
    }

    fun hasAccess(type: ContentAccessSubject, subjectId: String, role: ContentAccessRole): Boolean {
        return access.hasAccess(type, subjectId, role)
    }
}
