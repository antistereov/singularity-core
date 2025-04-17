package io.stereov.web.global.service.secrets.exception.model

import io.stereov.web.global.service.secrets.exception.SecretsException

class NoCurrentEncryptionKeyException : SecretsException(msg = "No current encryption key set.")
