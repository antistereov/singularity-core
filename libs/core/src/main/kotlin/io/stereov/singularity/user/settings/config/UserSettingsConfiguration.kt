package io.stereov.singularity.user.settings.config

import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.config.AuthenticationConfiguration
import io.stereov.singularity.auth.core.properties.SecurityAlertProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.EmailVerificationService
import io.stereov.singularity.auth.core.service.SecurityAlertService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.image.service.ImageStore
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.service.UserService
import io.stereov.singularity.user.settings.controller.UserSettingsController
import io.stereov.singularity.user.settings.service.UserSettingsService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration(after = [
    AuthenticationConfiguration::class
])
class UserSettingsConfiguration {

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun userSettingsController(
        userMapper: UserMapper,
        userSettingsService: UserSettingsService,
        cookieCreator: CookieCreator,
        authorizationService: AuthorizationService
    ) = UserSettingsController(userMapper, userSettingsService, cookieCreator, authorizationService)

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun userSettingsService(
        authService: AuthorizationService,
        emailVerificationService: EmailVerificationService,
        userService: UserService,
        hashService: HashService,
        fileStorage: FileStorage,
        accessTokenCache: AccessTokenCache,
        userMapper: UserMapper,
        emailProperties: EmailProperties,
        securityAlertProperties: SecurityAlertProperties,
        securityAlertService: SecurityAlertService,
        imageStore: ImageStore
    ) = UserSettingsService(
        authService,
        emailVerificationService,
        userService,
        hashService,
        fileStorage,
        accessTokenCache,
        userMapper,
        emailProperties,
        securityAlertProperties,
        securityAlertService,
        imageStore
    )
}
