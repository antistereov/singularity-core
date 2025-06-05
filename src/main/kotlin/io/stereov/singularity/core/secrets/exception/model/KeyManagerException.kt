package io.stereov.singularity.core.secrets.exception.model

import io.stereov.singularity.core.secrets.exception.SecretsException

class KeyManagerException(msg: String, cause: Throwable? = null) : SecretsException(msg, cause)
