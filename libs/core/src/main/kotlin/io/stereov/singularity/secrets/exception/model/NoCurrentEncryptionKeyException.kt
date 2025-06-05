package io.stereov.singularity.secrets.exception.model

import io.stereov.singularity.secrets.exception.SecretsException

class NoCurrentEncryptionKeyException : SecretsException(msg = "No current encryption key set.")
