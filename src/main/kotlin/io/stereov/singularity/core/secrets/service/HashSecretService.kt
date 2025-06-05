package io.stereov.singularity.core.secrets.service

import io.stereov.singularity.core.config.Constants
import io.stereov.singularity.core.secrets.component.KeyManager
import io.stereov.singularity.core.properties.AppProperties
import org.springframework.stereotype.Service

@Service
class HashSecretService(keyManager: KeyManager, appProperties: AppProperties) : SecretService(keyManager, Constants.JWT_SECRET, "HmacSHA256", appProperties)
