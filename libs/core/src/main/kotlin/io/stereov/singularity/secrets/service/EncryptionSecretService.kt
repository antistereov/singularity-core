package io.stereov.singularity.secrets.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.secrets.component.KeyManager
import org.springframework.stereotype.Service

@Service
class EncryptionSecretService(keyManager: KeyManager, appProperties: AppProperties) : SecretService(keyManager, Constants.ENCRYPTION_SECRET, "AES", appProperties) {

    override val logger = KotlinLogging.logger {}
}
