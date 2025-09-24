package io.stereov.singularity.auth.core.exception.model

import io.stereov.singularity.auth.core.exception.AuthException

class EmailAlreadyVerifiedException(msg: String, cause: Throwable? = null) : AuthException(msg, cause)