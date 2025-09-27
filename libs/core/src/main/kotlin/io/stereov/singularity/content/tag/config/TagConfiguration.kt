package io.stereov.singularity.content.tag.config

import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.content.core.config.ContentConfiguration
import io.stereov.singularity.content.core.properties.ContentProperties
import io.stereov.singularity.content.tag.controller.TagController
import io.stereov.singularity.content.tag.mapper.TagMapper
import io.stereov.singularity.content.tag.repository.TagRepository
import io.stereov.singularity.content.tag.service.TagService
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.translate.service.TranslateService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@AutoConfiguration(
    after = [
        ContentConfiguration::class
    ]
)
@EnableReactiveMongoRepositories(basePackageClasses = [TagRepository::class])

class TagConfiguration {

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun tagController(
        service: TagService,
        tagMapper: TagMapper,
        authorizationService: AuthorizationService
    ): TagController {
        return TagController(
            service,
            tagMapper,
            authorizationService
        )
    }

    // Mapper

    @Bean
    @ConditionalOnMissingBean
    fun tagMapper(translationService: TranslateService) = TagMapper(translationService)

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun tagService(
        repository: TagRepository,
        reactiveMongoTemplate: ReactiveMongoTemplate,
        contentProperties: ContentProperties,
        tagMapper: TagMapper,
        appProperties: AppProperties,
        authorizationService: AuthorizationService
    ): TagService {
        return TagService(
            repository,
            reactiveMongoTemplate,
            contentProperties,
            tagMapper,
            appProperties,
            authorizationService
        )
    }
}
