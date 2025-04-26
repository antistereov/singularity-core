package io.stereov.singularity.global.service.secrets.service

import io.stereov.singularity.config.Constants
import io.stereov.singularity.global.service.secrets.component.KeyManager
import io.stereov.singularity.properties.AppProperties
import org.springframework.stereotype.Service

@Service
class JwtSecretService(keyManager: KeyManager, appProperties: AppProperties) : SecretService(keyManager, Constants.JWT_SECRET, "HmacSHA256", appProperties)
