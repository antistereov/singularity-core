package io.stereov.singularity.secrets.core.exception.model

import io.stereov.singularity.secrets.core.exception.SecretsException

class NoCurrentEncryptionKeyException : SecretsException(msg = "No current encryption key set.")
