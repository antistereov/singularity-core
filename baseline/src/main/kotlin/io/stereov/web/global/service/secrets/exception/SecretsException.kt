package io.stereov.web.global.service.secrets.exception

import io.stereov.web.global.exception.BaseWebException

open class SecretsException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
