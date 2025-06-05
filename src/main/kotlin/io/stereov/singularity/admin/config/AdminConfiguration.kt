package io.stereov.singularity.admin.config

import io.stereov.singularity.admin.controller.AdminController
import io.stereov.singularity.admin.service.AdminService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.hash.config.HashConfiguration
import io.stereov.singularity.hash.service.HashService
import io.stereov.singularity.user.config.UserConfiguration
import io.stereov.singularity.user.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
        UserConfiguration::class,
        HashConfiguration::class,
    ]
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
