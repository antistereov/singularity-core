package io.stereov.singularity.secrets.core.exception

import io.stereov.singularity.global.exception.SingularityException

open class SecretsException(msg: String, cause: Throwable? = null) : SingularityException(msg, cause)
