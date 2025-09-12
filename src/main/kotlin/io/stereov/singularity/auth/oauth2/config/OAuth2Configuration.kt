package io.stereov.singularity.auth.oauth2.config

import io.stereov.singularity.auth.oauth2.exception.handler.OAuth2ExceptionHandler
import io.stereov.singularity.auth.oauth2.service.OAuth2Service
import io.stereov.singularity.auth.twofactor.properties.TwoFactorAuthProperties
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.user.core.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration(
    after = [
        ApplicationConfiguration::class
    ]
)
class OAuth2Configuration {

    // ExceptionHandler

    @Bean
    @ConditionalOnMissingBean
    fun oauth2ExceptionHandler() = OAuth2ExceptionHandler()

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun oauth2Service(
        userService: UserService,
        appProperties: AppProperties,
        twoFactorAuthProperties: TwoFactorAuthProperties
    ) = OAuth2Service(
        userService,
        appProperties,
        twoFactorAuthProperties
    )
}