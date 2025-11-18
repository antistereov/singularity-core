package io.stereov.singularity.database.encryption.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.core.service.SecretService
import org.springframework.stereotype.Service

/**
 * Service responsible for managing encryption-related secrets in the application.
 * Extends the [SecretService] class and is configured to handle secrets specifically for encryption purposes.
 * Uses AES encryption as the default algorithm and integrates with application properties for configuration.
 *
 * @constructor Initializes the service with required dependencies.
 * @param secretStore The secret store used for securely managing secrets.
 * @param appProperties Application-specific properties for configuration.
 */
@Service
class EncryptionSecretService(secretStore: SecretStore, appProperties: AppProperties) : SecretService(secretStore, Constants.ENCRYPTION_SECRET, "AES", appProperties) {

    override val logger = KotlinLogging.logger {}
}
