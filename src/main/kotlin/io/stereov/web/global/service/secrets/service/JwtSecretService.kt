package io.stereov.web.global.service.secrets.service

import io.stereov.web.config.Constants
import io.stereov.web.global.service.secrets.component.KeyManager
import io.stereov.web.properties.AppProperties
import org.springframework.stereotype.Service

@Service
class JwtSecretService(keyManager: KeyManager, appProperties: AppProperties) : SecretService(keyManager, Constants.JWT_SECRET, "HmacSHA256", appProperties)
