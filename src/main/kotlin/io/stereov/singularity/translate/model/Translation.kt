package io.stereov.singularity.translate.model

import java.util.*

data class Translation<C>(
    val locale: Locale,
    val translation: C
)
