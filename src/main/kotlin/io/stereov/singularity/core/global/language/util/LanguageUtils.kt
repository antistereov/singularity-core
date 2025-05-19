package io.stereov.singularity.core.global.language.util

import io.stereov.singularity.core.global.language.model.Language

inline fun <reified T: Enum<T>> String.asEnum(): T {
    return enumValueOf<T>(this.uppercase())
}

fun String.asLanguage(): Language {
    return Language.fromString(this)
}
