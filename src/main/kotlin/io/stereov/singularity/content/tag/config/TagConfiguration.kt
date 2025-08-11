package io.stereov.singularity.content.tag.config

import io.stereov.singularity.content.core.config.ContentConfiguration
import io.stereov.singularity.content.core.properties.ContentProperties
import io.stereov.singularity.content.tag.controller.TagController
import io.stereov.singularity.content.tag.repository.TagRepository
import io.stereov.singularity.content.tag.service.TagService
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
    fun tagController(service: TagService): TagController {
        return TagController(service)
    }

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun tagService(repository: TagRepository, reactiveMongoTemplate: ReactiveMongoTemplate, contentProperties: ContentProperties): TagService {
        return TagService(repository, reactiveMongoTemplate, contentProperties)
    }
}
