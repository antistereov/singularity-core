package io.stereov.web.global.service.secrets.service

import io.stereov.web.config.Constants
import io.stereov.web.global.service.secrets.component.KeyManager
import org.springframework.stereotype.Service

@Service
class EncryptionSecretService(keyManager: KeyManager) : SecretService(keyManager, Constants.ENCRYPTION_SECRET, "AES")
