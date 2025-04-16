package io.stereov.web.global.service.encryption.exception

import io.stereov.web.global.exception.BaseWebException

open class EncryptionException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
