package io.stereov.singularity.translate.model

import java.util.*

/**
 * Represents a translatable resource where translations are stored in a map
 * with a specific locale as the key and the corresponding content as the value.
 *
 * @param C the type of content being translated.
 *
 * @property translations A map of translations by [Locale].
 */
interface Translatable<C> {

    val translations: Map<Locale, C>
}
