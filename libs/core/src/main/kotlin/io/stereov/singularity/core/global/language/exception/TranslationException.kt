package io.stereov.singularity.core.global.language.exception

import io.stereov.singularity.core.global.exception.BaseWebException

open class TranslationException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
