package io.stereov.singularity.file.local.config

import io.stereov.singularity.file.core.config.StorageConfiguration
import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@AutoConfiguration(
    after = [
        StorageConfiguration::class,
    ]
)
@Configuration
@ConditionalOnProperty(prefix = "singularity.file.storage", value = ["type"], havingValue = "local", matchIfMissing = true)
@EnableConfigurationProperties(LocalFileStorageProperties::class)
class LocalFileStorageConfiguration {

}