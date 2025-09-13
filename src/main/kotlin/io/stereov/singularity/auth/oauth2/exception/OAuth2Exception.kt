package io.stereov.singularity.auth.oauth2.exception

import io.stereov.singularity.auth.core.exception.AuthException

open class OAuth2Exception(msg: String, cause: Throwable? = null): AuthException(msg, cause)
