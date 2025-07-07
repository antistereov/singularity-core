package io.stereov.singularity.secrets.core.exception

import io.stereov.singularity.global.exception.BaseWebException

open class SecretsException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
