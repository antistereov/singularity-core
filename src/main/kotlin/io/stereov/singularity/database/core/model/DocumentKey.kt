package io.stereov.singularity.database.core.model

import com.fasterxml.jackson.annotation.JsonValue

@JvmInline
value class DocumentKey(
    @JsonValue val value: String
) {

    override fun toString() = value
}