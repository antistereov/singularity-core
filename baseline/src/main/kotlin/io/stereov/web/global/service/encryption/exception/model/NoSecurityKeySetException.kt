package io.stereov.web.global.service.encryption.exception.model

import io.stereov.web.global.service.encryption.exception.EncryptionException

class NoSecurityKeySetException : EncryptionException(msg = "No current encryption key set. Please make sure that CURRENT_SECRET_KEY is correctly set as an environment variable.")
