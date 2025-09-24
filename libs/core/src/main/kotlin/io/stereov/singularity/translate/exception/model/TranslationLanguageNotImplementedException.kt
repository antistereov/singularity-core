package io.stereov.singularity.translate.exception.model

import io.stereov.singularity.translate.exception.TranslationException
import java.util.*

class TranslationLanguageNotImplementedException(locale: Locale) : TranslationException(
    "There is no $locale translation for the requested item"
)
