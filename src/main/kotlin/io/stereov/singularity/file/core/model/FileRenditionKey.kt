package io.stereov.singularity.file.core.model

import com.fasterxml.jackson.annotation.JsonValue

@JvmInline
value class FileRenditionKey(
    @JsonValue val value: String
) {
    override fun toString() = value
}