package io.stereov.singularity.core.global.exception.model

import io.stereov.singularity.core.global.exception.GlobalBaseWebException

class MissingFunctionParameterException(msg: String, cause: Throwable? = null) : GlobalBaseWebException(msg, cause)
