package io.stereov.singularity.core.global.language.model

import io.stereov.singularity.core.global.exception.model.DocumentNotFoundException

interface Translatable<C> {

    val translations: Map<Language, C>
    val primaryLanguage: Language

    fun translate(lang: Language? = null): Pair<Language, C> {
        return if (lang != null) {
            getSpecificTranslation(lang) ?: getDefaultTranslation()
        } else {
            getDefaultTranslation()
        }
    }

    fun getSpecificTranslation(lang: Language): Pair<Language, C>? {
        val translation = translations[lang]

        return translation?.let { lang to it }
    }

    fun getDefaultTranslation(): Pair<Language, C> {
        val en = translations[Language.EN]
        if (en != null) return Language.EN to en

        val primaryTranslation = translations[primaryLanguage]
        if (primaryTranslation != null) return primaryLanguage to primaryTranslation

        val firstLang = translations.keys.firstOrNull()
        val firstTranslation = translations[firstLang]

        if (firstLang == null || firstTranslation == null) {
            throw DocumentNotFoundException("The requested item does not contain any translations")
        }

        return firstLang to firstTranslation
    }
}
