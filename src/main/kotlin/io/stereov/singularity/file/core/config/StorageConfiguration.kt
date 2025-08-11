package io.stereov.singularity.file.core.config

import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.file.core.exception.handler.FileExceptionHandler
import io.stereov.singularity.file.core.properties.StorageProperties
import io.stereov.singularity.file.core.repository.FileMetadataRepository
import io.stereov.singularity.file.core.service.FileMetadataService
import io.stereov.singularity.global.config.ApplicationConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
    ]
)
@EnableReactiveMongoRepositories(basePackageClasses = [FileMetadataRepository::class])
@EnableConfigurationProperties(StorageProperties::class)
class StorageConfiguration {

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun fileMetaDataService(repository: FileMetadataRepository, authenticationService: AuthenticationService): FileMetadataService {
        return FileMetadataService(repository, authenticationService)
    }

    // Exception Handler

    @Bean
    fun fileExceptionHandler() = FileExceptionHandler()
}
