package io.stereov.singularity.auth.group.exception.model

import io.stereov.singularity.auth.group.exception.GroupException

class InvalidGroupTranslationException(msg: String, cause: Throwable? = null) : GroupException(msg, cause)
