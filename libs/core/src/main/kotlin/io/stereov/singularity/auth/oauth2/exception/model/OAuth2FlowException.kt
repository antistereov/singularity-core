package io.stereov.singularity.auth.oauth2.exception.model

import io.stereov.singularity.auth.oauth2.exception.OAuth2Exception

class OAuth2FlowException(
    val errorCode: String,
    msg: String,
    cause: Throwable? = null
) : OAuth2Exception(msg, cause)
