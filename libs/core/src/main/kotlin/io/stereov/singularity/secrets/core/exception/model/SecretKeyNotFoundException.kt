package io.stereov.singularity.secrets.core.exception.model

import io.stereov.singularity.secrets.core.exception.SecretsException

class SecretKeyNotFoundException(keyId: String) : SecretsException(msg = "Encryption cannot be started: secret key $keyId is not set")
