package io.stereov.singularity.auth.oauth2.exception.model

import io.stereov.singularity.auth.oauth2.exception.OAuth2Exception

class CannotDisconnectIdentityProviderException(msg: String, cause: Throwable? = null) : OAuth2Exception(msg, cause)
