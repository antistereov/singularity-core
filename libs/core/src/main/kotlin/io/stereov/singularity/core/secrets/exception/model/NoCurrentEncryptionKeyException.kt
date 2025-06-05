package io.stereov.singularity.core.secrets.exception.model

import io.stereov.singularity.core.secrets.exception.SecretsException

class NoCurrentEncryptionKeyException : SecretsException(msg = "No current encryption key set.")
