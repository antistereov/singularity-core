package io.stereov.singularity.principal.group.model

/**
 * Represents a translation for a [Group], containing localized text for the group's name and description.
 *
 * This data class is used to store human-readable translations of a group's properties for different locales.
 * Each translation consists of a `name` and an optional `description`.
 *
 * @property name The localized name of the group.
 * @property description The localized description of the group, defaulting to an empty string if not provided.
 */
data class GroupTranslation(
    val name: String,
    val description: String = "",
)
