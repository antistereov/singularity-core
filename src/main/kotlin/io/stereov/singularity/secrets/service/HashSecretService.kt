package io.stereov.singularity.secrets.service

import io.stereov.singularity.config.Constants
import io.stereov.singularity.properties.AppProperties
import io.stereov.singularity.secrets.component.KeyManager
import org.springframework.stereotype.Service

@Service
class HashSecretService(keyManager: KeyManager, appProperties: AppProperties) : SecretService(keyManager, Constants.HASH_SECRET, "HmacSHA256", appProperties)
