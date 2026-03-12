package io.stereov.singularity.content.core.model

import io.stereov.singularity.database.core.model.DocumentKey
import org.bson.types.ObjectId

sealed interface ContentAccessSubject {

    @JvmInline
    value class UserId(val value: ObjectId) : ContentAccessSubject

    @JvmInline
    value class GroupKey(val value: DocumentKey) : ContentAccessSubject
}
