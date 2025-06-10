package io.stereov.singularity.group.config

import io.stereov.singularity.auth.config.AuthenticationConfiguration
import io.stereov.singularity.auth.service.AuthenticationService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.group.controller.GroupController
import io.stereov.singularity.group.exception.handler.GroupExceptionHandler
import io.stereov.singularity.group.repository.GroupRepository
import io.stereov.singularity.group.service.GroupService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
        AuthenticationConfiguration::class
    ]
)
@EnableReactiveMongoRepositories(basePackageClasses = [GroupRepository::class])
class GroupConfiguration {

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun groupService(
        groupRepository: GroupRepository,
        appProperties: AppProperties,
        authenticationService: AuthenticationService,
        reactiveMongoTemplate: ReactiveMongoTemplate
    ): GroupService {
        return GroupService(groupRepository, appProperties, authenticationService, reactiveMongoTemplate)
    }

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun groupController(service: GroupService) = GroupController(service)

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun groupExceptionHandler() = GroupExceptionHandler()
}
