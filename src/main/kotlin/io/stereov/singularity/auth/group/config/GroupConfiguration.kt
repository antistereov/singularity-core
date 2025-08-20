package io.stereov.singularity.auth.group.config

import io.stereov.singularity.auth.core.config.AuthenticationConfiguration
import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.auth.group.controller.GroupController
import io.stereov.singularity.auth.group.controller.GroupMemberController
import io.stereov.singularity.auth.group.exception.handler.GroupExceptionHandler
import io.stereov.singularity.auth.group.repository.GroupRepository
import io.stereov.singularity.auth.group.service.GroupMemberService
import io.stereov.singularity.auth.group.service.GroupService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.service.UserService
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

    @Bean
    @ConditionalOnMissingBean
    fun groupMemberService(
        userService: UserService,
        groupService: GroupService,
        authService: AuthenticationService
    ) = GroupMemberService(userService, groupService, authService)

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun groupController(service: GroupService) = GroupController(service)

    @Bean
    @ConditionalOnMissingBean
    fun groupMemberController(
        service: GroupMemberService,
        userMapper: UserMapper
    ) = GroupMemberController(service, userMapper)

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun groupExceptionHandler() = GroupExceptionHandler()
}
