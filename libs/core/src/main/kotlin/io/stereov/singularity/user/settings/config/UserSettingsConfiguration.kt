package io.stereov.singularity.user.settings.config

import io.stereov.singularity.auth.core.config.AuthenticationConfiguration
import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.auth.core.service.CookieCreator
import io.stereov.singularity.auth.session.cache.AccessTokenCache
import io.stereov.singularity.auth.twofactor.service.StepUpTokenService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.mail.user.service.UserMailSender
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
        cookieCreator: CookieCreator
    ) = UserSettingsController(userMapper, userSettingsService, cookieCreator)

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun userSettingsService(
        authService: AuthenticationService,
        stepUpTokenService: StepUpTokenService,
        userMailSender: UserMailSender,
        userService: UserService,
        hashService: HashService,
        appProperties: AppProperties,
        fileStorage: FileStorage,
        accessTokenCache: AccessTokenCache,
        userMapper: UserMapper
    ) = UserSettingsService(authService, stepUpTokenService, userMailSender, userService, hashService, appProperties, fileStorage, accessTokenCache, userMapper)
}
