package io.stereov.singularity.admin.config

import io.stereov.singularity.admin.controller.AdminController
import io.stereov.singularity.admin.service.AdminService
import io.stereov.singularity.config.ApplicationConfiguration
import io.stereov.singularity.group.repository.GroupRepository
import io.stereov.singularity.hash.HashService
import io.stereov.singularity.properties.AppProperties
import io.stereov.singularity.user.config.UserConfiguration
import io.stereov.singularity.user.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
        UserConfiguration::class,
    ]
)
@EnableReactiveMongoRepositories(
    basePackageClasses = [GroupRepository::class]
)
class AdminConfiguration {

    // Services

    @Bean
    @ConditionalOnMissingBean
    fun adminService(context: ApplicationContext, userService: UserService, hashService: HashService, appProperties: AppProperties): AdminService {
        return AdminService(context, userService, appProperties, hashService)
    }

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun adminController(adminService: AdminService, userService: UserService): AdminController {
        return AdminController(adminService, userService)
    }
}
