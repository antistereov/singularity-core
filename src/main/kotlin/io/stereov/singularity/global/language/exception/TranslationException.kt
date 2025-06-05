package io.stereov.singularity.global.language.exception

import io.stereov.singularity.global.exception.BaseWebException

open class TranslationException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
