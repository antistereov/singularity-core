package io.stereov.singularity.file.core.config

import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.core.component.AccessCriteria
import io.stereov.singularity.content.core.properties.ContentProperties
import io.stereov.singularity.file.core.component.DataBufferPublisher
import io.stereov.singularity.file.core.controller.FileController
import io.stereov.singularity.file.core.mapper.FileMetadataMapper
import io.stereov.singularity.file.core.properties.StorageProperties
import io.stereov.singularity.file.core.repository.FileMetadataRepository
import io.stereov.singularity.file.core.service.FileMetadataService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.download.service.DownloadService
import io.stereov.singularity.file.image.service.ImageStore
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.translate.service.TranslateService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
    ]
)
@EnableReactiveMongoRepositories(basePackageClasses = [FileMetadataRepository::class])
@EnableConfigurationProperties(StorageProperties::class)
class StorageConfiguration {

    // Component

    @Bean
    @ConditionalOnMissingBean
    fun dataBufferPublisher() = DataBufferPublisher()

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun fileController(
        fileStorage: FileStorage,
        imageStore: ImageStore,
        authorizationService: AuthorizationService,
        fileMetadataService: FileMetadataService
    ) = FileController(
        fileStorage,
        imageStore,
        authorizationService,
        fileMetadataService
    )

    // Mapper

    @Bean
    @ConditionalOnMissingBean
    fun fileMetadataMapper(
        appProperties: AppProperties
    ) = FileMetadataMapper(appProperties)

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun downloadService(webClient: WebClient) = DownloadService(webClient)

    @Bean
    @ConditionalOnMissingBean
    fun fileMetaDataService(
        repository: FileMetadataRepository,
        authorizationService: AuthorizationService,
        reactiveMongoTemplate: ReactiveMongoTemplate,
        translateService: TranslateService,
        accessCriteria: AccessCriteria,
        contentProperties: ContentProperties
    ): FileMetadataService {
        return FileMetadataService(
            repository,
            authorizationService,
            reactiveMongoTemplate,
            translateService,
            accessCriteria,
            contentProperties
        )
    }
}
