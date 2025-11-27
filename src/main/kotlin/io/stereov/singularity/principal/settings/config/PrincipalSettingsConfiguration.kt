package io.stereov.singularity.principal.settings.config

import io.stereov.singularity.auth.alert.properties.SecurityAlertProperties
import io.stereov.singularity.auth.alert.service.SecurityAlertService
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.config.AuthenticationConfiguration
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.EmailVerificationService
import io.stereov.singularity.auth.token.component.CookieCreator
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.image.service.ImageStore
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.service.PrincipalService
import io.stereov.singularity.principal.core.service.UserService
import io.stereov.singularity.principal.settings.controller.PrincipalSettingsController
import io.stereov.singularity.principal.settings.service.PrincipalSettingsService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration(after = [
    AuthenticationConfiguration::class
])
class PrincipalSettingsConfiguration {

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun principalSettingsController(
        principalMapper: PrincipalMapper,
        principalSettingsService: PrincipalSettingsService,
        cookieCreator: CookieCreator,
        authorizationService: AuthorizationService,
        principalService: PrincipalService,
        userService: UserService,
        accessTokenCache: AccessTokenCache
    ) = PrincipalSettingsController(
        principalMapper,
        principalSettingsService,
        cookieCreator,
        authorizationService,
        principalService,
        userService,
        accessTokenCache
    )

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun principalSettingsService(
        emailVerificationService: EmailVerificationService,
        userService: UserService,
        hashService: HashService,
        fileStorage: FileStorage,
        emailProperties: EmailProperties,
        securityAlertProperties: SecurityAlertProperties,
        securityAlertService: SecurityAlertService,
        imageStore: ImageStore,
        principalService: PrincipalService
    ) = PrincipalSettingsService(
        emailVerificationService,
        userService,
        hashService,
        fileStorage,
        emailProperties,
        securityAlertProperties,
        securityAlertService,
        imageStore,
        principalService,
    )
}
