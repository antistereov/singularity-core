package io.stereov.singularity.content.translate.exception.model

import io.stereov.singularity.content.translate.exception.TranslationException
import io.stereov.singularity.content.translate.model.Language

class TranslationForLangMissingException(lang: Language) : TranslationException("There is no $lang translation for the requested item.")
