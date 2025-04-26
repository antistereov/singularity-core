package io.stereov.singularity.core.config.storage

import io.stereov.singularity.core.config.ApplicationConfiguration
import io.stereov.singularity.core.properties.storage.StorageProperties
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
