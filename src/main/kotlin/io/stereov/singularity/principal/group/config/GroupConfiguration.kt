package io.stereov.singularity.principal.group.config

import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.config.AuthenticationConfiguration
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.service.UserService
import io.stereov.singularity.principal.group.controller.GroupController
import io.stereov.singularity.principal.group.controller.GroupMemberController
import io.stereov.singularity.principal.group.mapper.GroupMapper
import io.stereov.singularity.principal.group.repository.GroupRepository
import io.stereov.singularity.principal.group.service.GroupMemberService
import io.stereov.singularity.principal.group.service.GroupService
import io.stereov.singularity.translate.service.TranslateService
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

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun groupController(
        service: GroupService,
        groupMapper: GroupMapper,
        authorizationService: AuthorizationService,
    ) = GroupController(
        service,
        groupMapper,
        authorizationService,
    )

    @Bean
    @ConditionalOnMissingBean
    fun groupMemberController(
        service: GroupMemberService,
        principalMapper: PrincipalMapper,
        authorizationService: AuthorizationService
    ) = GroupMemberController(
        service,
        principalMapper,
        authorizationService
    )

    // Mapper

    @Bean
    @ConditionalOnMissingBean
    fun groupMapper(
        translateService: TranslateService
    ) = GroupMapper(translateService)

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun groupService(
        groupRepository: GroupRepository,
        appProperties: AppProperties,
        reactiveMongoTemplate: ReactiveMongoTemplate,
        groupMapper: GroupMapper,
        userService: UserService
    ): GroupService {
        return GroupService(
            groupRepository,
            appProperties,
            reactiveMongoTemplate,
            groupMapper,
            userService
        )
    }

    @Bean
    @ConditionalOnMissingBean
    fun groupMemberService(
        userService: UserService,
        groupService: GroupService,
        accessTokenCache: AccessTokenCache
    ) = GroupMemberService(userService, groupService, accessTokenCache)
}
