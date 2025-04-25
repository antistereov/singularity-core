package io.stereov.web.config.storage

import io.stereov.web.config.ApplicationConfiguration
import io.stereov.web.properties.storage.StorageProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
    ]
)
@EnableConfigurationProperties(StorageProperties::class)
class StorageConfiguration
