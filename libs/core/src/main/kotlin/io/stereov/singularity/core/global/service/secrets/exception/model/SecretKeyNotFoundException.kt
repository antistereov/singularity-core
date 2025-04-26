package io.stereov.singularity.core.global.service.secrets.exception.model

import io.stereov.singularity.core.global.service.secrets.exception.SecretsException

class SecretKeyNotFoundException(keyId: String) : SecretsException(msg = "Encryption cannot be started: secret key $keyId is not set")
