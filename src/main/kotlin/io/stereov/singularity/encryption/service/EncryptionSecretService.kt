package io.stereov.singularity.encryption.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.core.service.SecretService
import org.springframework.stereotype.Service

@Service
class EncryptionSecretService(secretStore: SecretStore, appProperties: AppProperties) : SecretService(secretStore, Constants.ENCRYPTION_SECRET, "AES", appProperties) {

    override val logger = KotlinLogging.logger {}
}