package io.stereov.singularity.core.global.language.model

import io.stereov.singularity.core.global.language.exception.model.TranslationForLangMissingException

interface Translatable<C> {

    val translations: Map<Language, C>

    fun translate(lang: Language): C {
        return translations[lang] ?: throw TranslationForLangMissingException(lang)
    }
}
