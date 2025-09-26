package io.stereov.singularity.content.core.model

enum class ContentAccessRole {
    VIEWER, EDITOR, MAINTAINER;

    companion object {
        fun fromString(string: String): ContentAccessRole {
            return when (string.lowercase()) {
                VIEWER.toString().lowercase() -> VIEWER
                EDITOR.toString().lowercase() -> EDITOR
                MAINTAINER.toString().lowercase() -> MAINTAINER
                else -> throw IllegalArgumentException("Invalid content access role: $string")
            }
        }
    }
}
