package io.stereov.singularity.database.hash.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.core.service.SecretService
import org.springframework.stereotype.Service

/**
 * A service for managing HMAC-based secrets leveraging the `HmacSHA256` algorithm.
 * This service provides secure handling and retrieval of secrets related to hashing operations,
 * ensuring that they are fixed and protected throughout their lifecycle.
 *
 * Inherits functionality from the base `SecretService` class, which handles core behaviors for secret management.
 *
 *
 * @param secretStore The underlying store for managing secrets securely with caching capabilities.
 * @param appProperties Application-wide properties that provide configuration details.
 */
@Service
class HashSecretService(
    secretStore: SecretStore,
    appProperties: AppProperties
) : SecretService(
    secretStore,
    Constants.HASH_SECRET,
    "HmacSHA256",
    appProperties,
    fixSecret = true
) {

    override val logger = KotlinLogging.logger {}
}
