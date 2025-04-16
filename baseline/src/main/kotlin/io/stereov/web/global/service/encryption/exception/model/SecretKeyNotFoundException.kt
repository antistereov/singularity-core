package io.stereov.web.global.service.encryption.exception.model

import io.stereov.web.global.service.encryption.exception.EncryptionException

class SecretKeyNotFoundException(keyId: String) : EncryptionException(msg = "Encryption cannot be started: secret key $keyId is not set")
