package io.stereov.web.global.service.secrets.exception.model

import io.stereov.web.global.service.secrets.exception.SecretsException

class KeyManagerException(msg: String, cause: Throwable? = null) : SecretsException(msg, cause)
