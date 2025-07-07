package io.stereov.singularity.jwt.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.util.Constants
import io.stereov.singularity.secrets.core.component.KeyManager
import io.stereov.singularity.secrets.core.service.SecretService
import org.springframework.stereotype.Service

@Service
class JwtSecretService(keyManager: KeyManager, appProperties: AppProperties) : SecretService(keyManager, Constants.JWT_SECRET, "HmacSHA256", appProperties) {

    override val logger = KotlinLogging.logger {}
}