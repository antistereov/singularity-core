package io.stereov.singularity.content.translate.exception

import io.stereov.singularity.global.exception.BaseWebException

open class TranslationException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
