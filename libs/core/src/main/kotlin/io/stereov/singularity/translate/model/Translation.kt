package io.stereov.singularity.translate.model

import java.util.*

/**
 * Represents a translation of a specific content within a particular locale.
 *
 * This data structure is used to pair a piece of content with the locale
 * it is associated with, facilitating localized translations.
 *
 * @param C The type of content being translated.
 *
 * @property locale The locale associated with the translation (e.g., language and region).
 * @property translation The translated content for the given locale.
 */
data class Translation<C>(
    val locale: Locale,
    val translation: C
)
