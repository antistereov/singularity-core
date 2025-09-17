package io.stereov.singularity.translate.model

import java.util.*

interface Translatable<C> {

    val translations: Map<Locale, C>
    val primaryLocale: Locale
}
