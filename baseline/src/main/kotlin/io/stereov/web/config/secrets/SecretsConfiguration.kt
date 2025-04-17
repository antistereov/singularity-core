package io.stereov.web.config.secrets

import io.stereov.web.config.ApplicationConfiguration
import io.stereov.web.properties.secrets.KeyManagerProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
    ]
)
@EnableConfigurationProperties(KeyManagerProperties::class)
class SecretsConfiguration
