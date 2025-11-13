package io.stereov.singularity.auth.oauth2.exception

import io.stereov.singularity.global.exception.SingularityException

open class OAuth2Exception(msg: String, cause: Throwable? = null): SingularityException(msg, cause)
