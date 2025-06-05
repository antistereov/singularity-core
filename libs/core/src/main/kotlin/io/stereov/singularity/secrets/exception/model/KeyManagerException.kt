package io.stereov.singularity.secrets.exception.model

import io.stereov.singularity.secrets.exception.SecretsException

class KeyManagerException(msg: String, cause: Throwable? = null) : SecretsException(msg, cause)
