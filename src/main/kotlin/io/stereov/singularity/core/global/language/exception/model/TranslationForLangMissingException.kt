package io.stereov.singularity.core.global.language.exception.model

import io.stereov.singularity.core.global.language.exception.TranslationException
import io.stereov.singularity.core.global.language.model.Language

class TranslationForLangMissingException(lang: Language) : TranslationException("There is no $lang translation for the requested item.")
