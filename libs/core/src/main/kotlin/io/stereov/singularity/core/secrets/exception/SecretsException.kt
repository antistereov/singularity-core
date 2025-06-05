package io.stereov.singularity.core.secrets.exception

import io.stereov.singularity.core.global.exception.BaseWebException

open class SecretsException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
