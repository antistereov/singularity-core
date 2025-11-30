package io.stereov.singularity.admin.core.config

import io.stereov.singularity.admin.core.controller.AdminController
import io.stereov.singularity.admin.core.service.AdminService
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.database.hash.config.HashConfiguration
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.service.PrincipalService
import io.stereov.singularity.principal.core.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
        HashConfiguration::class,
    ]
)
class AdminConfiguration {

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun adminController(
        adminService: AdminService,
        userService: UserService,
        authorizationService: AuthorizationService,
        principalMapper: PrincipalMapper,
        accessTokenCache: AccessTokenCache,
        principalService: PrincipalService
    ): AdminController {
        return AdminController(
            adminService,
            accessTokenCache,
            authorizationService,
            userService,
            principalMapper,
            principalService
        )
    }

    // Services

    @Bean
    @ConditionalOnMissingBean
    fun adminService(
        userService: UserService,
        hashService: HashService,
        appProperties: AppProperties,
        twoFactorEmailCodeProperties: TwoFactorEmailCodeProperties,
        emailProperties: EmailProperties,
    ): AdminService {
        return AdminService(
            userService,
            appProperties,
            hashService,
            twoFactorEmailCodeProperties,
            emailProperties,
        )
    }
}
