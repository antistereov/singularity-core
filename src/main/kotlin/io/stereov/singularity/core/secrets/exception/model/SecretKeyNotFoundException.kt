package io.stereov.singularity.core.secrets.exception.model

import io.stereov.singularity.core.secrets.exception.SecretsException

class SecretKeyNotFoundException(keyId: String) : SecretsException(msg = "Encryption cannot be started: secret key $keyId is not set")
