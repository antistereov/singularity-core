package io.stereov.singularity.global.exception.model

import io.stereov.singularity.global.exception.GlobalBaseWebException

class MissingRequestParameterException(msg: String, cause: Throwable? = null) : GlobalBaseWebException(msg, cause)