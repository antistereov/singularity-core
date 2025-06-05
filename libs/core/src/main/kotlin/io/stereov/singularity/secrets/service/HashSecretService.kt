package io.stereov.singularity.secrets.service

import io.stereov.singularity.config.Constants
import io.stereov.singularity.secrets.component.KeyManager
import io.stereov.singularity.properties.AppProperties
import org.springframework.stereotype.Service

@Service
class HashSecretService(keyManager: KeyManager, appProperties: AppProperties) : SecretService(keyManager, Constants.JWT_SECRET, "HmacSHA256", appProperties)
