package io.stereov.singularity.secrets.exception

import io.stereov.singularity.global.exception.BaseWebException

open class SecretsException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
