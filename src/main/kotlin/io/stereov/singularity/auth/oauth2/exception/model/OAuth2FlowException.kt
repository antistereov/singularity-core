package io.stereov.singularity.auth.oauth2.exception.model

import io.stereov.singularity.auth.oauth2.exception.OAuth2Exception
import io.stereov.singularity.auth.oauth2.model.OAuth2ErrorCode

class OAuth2FlowException(
    val errorCode: OAuth2ErrorCode,
    msg: String,
    cause: Throwable? = null
) : OAuth2Exception(msg, cause)
