package io.stereov.singularity.group.config

import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.group.exception.handler.GroupExceptionHandler
import io.stereov.singularity.group.repository.GroupRepository
import io.stereov.singularity.group.service.GroupService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@AutoConfiguration(
    after = [
        ApplicationConfiguration::class
    ]
)
@EnableReactiveMongoRepositories(basePackageClasses = [GroupRepository::class])
class GroupConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun groupService(
        groupRepository: GroupRepository,
        appProperties: AppProperties
    ): GroupService {
        return GroupService(groupRepository, appProperties)
    }

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun groupExceptionHandler() = GroupExceptionHandler()
}
