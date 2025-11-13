package io.stereov.singularity.translate.exception

import io.stereov.singularity.global.exception.SingularityException

open class TranslationException(msg: String, cause: Throwable? = null) : SingularityException(msg, cause)
