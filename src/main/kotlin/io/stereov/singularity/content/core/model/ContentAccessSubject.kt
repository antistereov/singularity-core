package io.stereov.singularity.content.core.model

import com.fasterxml.jackson.annotation.JsonValue
import io.stereov.singularity.database.core.model.DocumentKey
import org.bson.types.ObjectId

sealed interface ContentAccessSubject {

    @JvmInline
    value class UserId(
        @JsonValue val value: ObjectId
    ) : ContentAccessSubject {

        override fun toString(): String {
            return value.toString()
        }
    }

    @JvmInline
    value class GroupKey(
        @JsonValue val value: DocumentKey
    ) : ContentAccessSubject {

        override fun toString(): String {
            return value.toString()
        }
    }
}
