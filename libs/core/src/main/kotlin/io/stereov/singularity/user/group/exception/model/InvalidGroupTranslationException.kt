package io.stereov.singularity.user.group.exception.model

import io.stereov.singularity.user.group.exception.GroupException

class InvalidGroupTranslationException(msg: String, cause: Throwable? = null) : GroupException(msg, cause)
