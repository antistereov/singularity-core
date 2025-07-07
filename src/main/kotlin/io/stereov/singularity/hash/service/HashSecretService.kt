package io.stereov.singularity.hash.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.secrets.core.component.KeyManager
import io.stereov.singularity.secrets.core.service.SecretService
import org.springframework.stereotype.Service

@Service
class HashSecretService(
    keyManager: KeyManager,
    appProperties: AppProperties
) : SecretService(
    keyManager,
    Constants.HASH_SECRET,
    "HmacSHA256",
    appProperties,
    fixSecret = true
) {

    override val logger = KotlinLogging.logger {}
}