package io.stereov.singularity.core.global.language.exception.model

import io.stereov.singularity.core.global.language.exception.TranslationException

class TranslationLanguageNotImplementedException(lang: String) : TranslationException(
    "There is no $lang translation for the requested item"
)
