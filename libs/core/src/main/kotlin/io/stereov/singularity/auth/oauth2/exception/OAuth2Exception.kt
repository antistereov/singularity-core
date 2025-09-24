package io.stereov.singularity.auth.oauth2.exception

import io.stereov.singularity.global.exception.BaseWebException

open class OAuth2Exception(msg: String, cause: Throwable? = null): BaseWebException(msg, cause)
