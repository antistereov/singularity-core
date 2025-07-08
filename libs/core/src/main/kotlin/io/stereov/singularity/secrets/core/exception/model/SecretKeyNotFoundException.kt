package io.stereov.singularity.secrets.core.exception.model

import io.stereov.singularity.secrets.core.exception.SecretsException

class SecretKeyNotFoundException(key: String) : SecretsException(msg = "Encryption cannot be started: secret key $key is not set")
