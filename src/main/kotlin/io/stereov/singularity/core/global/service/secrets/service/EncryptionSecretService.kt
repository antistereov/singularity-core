package io.stereov.singularity.core.global.service.secrets.service

import io.stereov.singularity.core.config.Constants
import io.stereov.singularity.core.global.service.secrets.component.KeyManager
import io.stereov.singularity.core.properties.AppProperties
import org.springframework.stereotype.Service

@Service
class EncryptionSecretService(keyManager: KeyManager, appProperties: AppProperties) : SecretService(keyManager, Constants.ENCRYPTION_SECRET, "AES", appProperties)
