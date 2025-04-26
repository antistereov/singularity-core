package io.stereov.singularity.core.global.service.secrets.exception.model

import io.stereov.singularity.core.global.service.secrets.exception.SecretsException

class NoCurrentEncryptionKeyException : SecretsException(msg = "No current encryption key set.")
