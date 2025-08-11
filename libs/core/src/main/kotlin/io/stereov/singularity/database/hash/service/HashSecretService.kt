package io.stereov.singularity.database.hash.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.core.service.SecretService
import org.springframework.stereotype.Service

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
