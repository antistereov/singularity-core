package io.stereov.singularity.auth.jwt.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.core.service.SecretService
import org.springframework.stereotype.Service

/**
 * Service for managing JWT secret keys. Extends the functionality provided by the base `SecretService`
 * to handle secrets specific to JWT authentication using HMAC SHA-256 algorithm.
 *
 * This class is responsible for retrieving and managing the secret required for JWT signing,
 * leveraging the provided `SecretStore` for secure storage and retrieval, and using `AppProperties`
 * for application-specific configurations.
 *
 * @constructor Creates a new instance of the service.
 * @param secretStore The `SecretStore` implementation used to manage secrets.
 * @param appProperties Application-level properties required for configuration.
 *
 * @see SecretService
 * @see SecretStore
 * @see AppProperties
 */
@Service
class JwtSecretService(secretStore: SecretStore, appProperties: AppProperties) : SecretService(secretStore, Constants.JWT_SECRET, "HmacSHA256", appProperties) {

    override val logger = KotlinLogging.logger {}
}
