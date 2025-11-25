package io.stereov.singularity.auth.oauth2.config

import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.token.component.CookieCreator
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.alert.properties.SecurityAlertProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.alert.service.IdentityProviderInfoService
import io.stereov.singularity.auth.alert.service.SecurityAlertService
import io.stereov.singularity.auth.token.service.AccessTokenService
import io.stereov.singularity.auth.token.service.StepUpTokenService
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.oauth2.controller.IdentityProviderController
import io.stereov.singularity.auth.oauth2.controller.OAuth2ProviderController
import io.stereov.singularity.auth.oauth2.exception.handler.OAuth2ExceptionHandler
import io.stereov.singularity.auth.oauth2.properties.OAuth2Properties
import io.stereov.singularity.auth.oauth2.service.IdentityProviderService
import io.stereov.singularity.auth.oauth2.service.OAuth2AuthenticationService
import io.stereov.singularity.auth.oauth2.service.token.OAuth2ProviderConnectionTokenService
import io.stereov.singularity.auth.oauth2.service.token.OAuth2StateTokenService
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.file.download.service.DownloadService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.service.UserService
import io.stereov.singularity.user.settings.service.UserSettingsService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration(
    after = [
        ApplicationConfiguration::class
    ]
)
@EnableConfigurationProperties(OAuth2Properties::class)
class OAuth2Configuration {
    
    // Controller

    @Bean
    @ConditionalOnProperty("singularity.auth.oauth2.enable", matchIfMissing = false)
    @ConditionalOnMissingBean
    fun identityProviderController(
        identityProviderService: IdentityProviderService,
        authorizationService: AuthorizationService,
        userMapper: UserMapper,
    ) = IdentityProviderController(
        identityProviderService,
        authorizationService,
        userMapper,
    )

    @Bean
    @ConditionalOnProperty("singularity.auth.oauth2.enable", matchIfMissing = false)
    @ConditionalOnMissingBean
    fun oauth2ProviderController(
        authorizationService: AuthorizationService,
        oAuth2ProviderConnectionTokenService: OAuth2ProviderConnectionTokenService,
        authProperties: AuthProperties,
        cookieCreator: CookieCreator
    ) = OAuth2ProviderController(
        authorizationService,
        oAuth2ProviderConnectionTokenService,
        authProperties,
        cookieCreator
    )

    // ExceptionHandler

    @Bean
    @ConditionalOnMissingBean
    fun oauth2ExceptionHandler() = OAuth2ExceptionHandler()

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun identityProviderService(
        userService: UserService,
        oAuth2ProviderConnectionTokenService: OAuth2ProviderConnectionTokenService,
        authorizationService: AuthorizationService,
        hashService: HashService,
        accessTokenCache: AccessTokenCache,
        accessTokenService: AccessTokenService,
        stepUpTokenService: StepUpTokenService,
        emailProperties: EmailProperties,
        securityAlertService: SecurityAlertService,
        securityAlertProperties: SecurityAlertProperties
    ) = IdentityProviderService(
        userService,
        oAuth2ProviderConnectionTokenService,
        authorizationService,
        hashService,
        accessTokenCache,
        accessTokenService,
        stepUpTokenService,
        emailProperties,
        securityAlertService,
        securityAlertProperties
    )
    
    @Bean
    @ConditionalOnMissingBean
    fun oauth2ProviderConnectionTokenService(
        jwtService: JwtService,
        jwtProperties: JwtProperties,
    ) = OAuth2ProviderConnectionTokenService(
        jwtService,
        jwtProperties,
    )

    @Bean
    @ConditionalOnMissingBean
    fun oauth2StateTokenService(
        jwtService: JwtService,
        jwtProperties: JwtProperties
    ) = OAuth2StateTokenService(jwtService, jwtProperties)

    @Bean
    @ConditionalOnMissingBean
    fun oAuth2AuthenticationService(
        userService: UserService,
        twoFactorEmailCodeProperties: TwoFactorEmailCodeProperties,
        identityProviderService: IdentityProviderService,
        accessTokenService: AccessTokenService,
        userSettingsService: UserSettingsService,
        downloadService: DownloadService,
        identityProviderInfoService: IdentityProviderInfoService,
        emailProperties: EmailProperties
    ) = OAuth2AuthenticationService(
        userService,
        twoFactorEmailCodeProperties,
        identityProviderService,
        accessTokenService,
        userSettingsService,
        downloadService,
        identityProviderInfoService,
        emailProperties,
    )
}
