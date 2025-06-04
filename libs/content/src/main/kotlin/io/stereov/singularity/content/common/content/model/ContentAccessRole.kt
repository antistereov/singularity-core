package io.stereov.singularity.content.common.content.model

enum class ContentAccessRole {
    VIEWER, EDITOR, ADMIN;

    companion object {
        fun fromString(string: String): ContentAccessRole {
            return when (string.lowercase()) {
                VIEWER.toString().lowercase() -> VIEWER
                EDITOR.toString().lowercase() -> EDITOR
                ADMIN.toString().lowercase() -> ADMIN
                else -> throw IllegalArgumentException("Invalid content access role: $string")
            }
        }
    }
}
