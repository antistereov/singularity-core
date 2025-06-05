package io.stereov.singularity.translate.exception.model

import io.stereov.singularity.translate.exception.TranslationException
import io.stereov.singularity.translate.model.Language

class TranslationForLangMissingException(lang: Language) : TranslationException("There is no $lang translation for the requested item.")
