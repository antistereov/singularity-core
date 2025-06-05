package io.stereov.singularity.translate.exception.model

import io.stereov.singularity.translate.exception.TranslationException

class TranslationLanguageNotImplementedException(lang: String) : TranslationException(
    "There is no $lang translation for the requested item"
)
