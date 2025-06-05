package io.stereov.singularity.file.config

import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.file.properties.StorageProperties
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
