package io.stereov.singularity.content.tag.model

/**
 * Represents a translation for a tag, including a name and an optional description.
 *
 * This class is used to store localized content for tags, allowing support for multiple languages.
 *
 * @property name The localized name of the tag. This field is required.
 * @property description An optional localized description of the tag. Defaults to an empty string.
 */
data class TagTranslation(
    var name: String,
    var description: String = ""
)
