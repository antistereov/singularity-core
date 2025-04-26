package io.stereov.singularity.global.service.secrets.exception.model

import io.stereov.singularity.global.service.secrets.exception.SecretsException

class NoCurrentEncryptionKeyException : SecretsException(msg = "No current encryption key set.")
