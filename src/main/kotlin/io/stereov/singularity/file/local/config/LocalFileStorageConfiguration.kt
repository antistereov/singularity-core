package io.stereov.singularity.file.local.config

import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.file.core.config.StorageConfiguration
import io.stereov.singularity.file.core.mapper.FileMetadataMapper
import io.stereov.singularity.file.core.properties.StorageProperties
import io.stereov.singularity.file.core.service.FileMetadataService
import io.stereov.singularity.file.local.controller.LocalFileStorageController
import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import io.stereov.singularity.file.local.service.LocalFileStorage
import io.stereov.singularity.global.properties.AppProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@AutoConfiguration(
    after = [
        StorageConfiguration::class,
    ]
)
@Configuration
@ConditionalOnProperty(prefix = "singularity.file.storage", value = ["type"], havingValue = "local", matchIfMissing = true)
@EnableConfigurationProperties(LocalFileStorageProperties::class)
internal class LocalFileStorageConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun fileStorage(
        properties: LocalFileStorageProperties,
        appProperties: AppProperties,
        metadataService: FileMetadataService,
        fileMetadataMapper: FileMetadataMapper,
        storageProperties: StorageProperties
    ): LocalFileStorage {
        return LocalFileStorage(
            properties,
            appProperties,
            metadataService,
            fileMetadataMapper,
            storageProperties
        )
    }

    @Bean
    fun localFileStorageController(
        localFileStorage: LocalFileStorage,
        authorizationService: AuthorizationService,
    ) = LocalFileStorageController(localFileStorage, authorizationService)
}
