package io.stereov.singularity.secrets.core.exception.model

import io.stereov.singularity.secrets.core.exception.SecretsException

class SecretStoreException(msg: String, cause: Throwable? = null) : SecretsException(msg, cause)
