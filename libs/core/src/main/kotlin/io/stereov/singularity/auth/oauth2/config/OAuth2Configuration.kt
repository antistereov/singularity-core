package io.stereov.singularity.auth.oauth2.config

import io.stereov.singularity.auth.alert.properties.SecurityAlertProperties
import io.stereov.singularity.auth.alert.service.IdentityProviderInfoService
import io.stereov.singularity.auth.alert.service.SecurityAlertService
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.oauth2.controller.IdentityProviderController
import io.stereov.singularity.auth.oauth2.controller.OAuth2ProviderController
import io.stereov.singularity.auth.oauth2.properties.OAuth2Properties
import io.stereov.singularity.auth.oauth2.service.IdentityProviderService
import io.stereov.singularity.auth.oauth2.service.OAuth2AuthenticationService
import io.stereov.singularity.auth.token.component.CookieCreator
import io.stereov.singularity.auth.token.service.AccessTokenService
import io.stereov.singularity.auth.token.service.OAuth2ProviderConnectionTokenService
import io.stereov.singularity.auth.token.service.OAuth2StateTokenService
import io.stereov.singularity.auth.token.service.StepUpTokenService
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.file.download.service.DownloadService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.service.PrincipalService
import io.stereov.singularity.principal.core.service.UserService
import io.stereov.singularity.principal.settings.service.PrincipalSettingsService
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
        principalMapper: PrincipalMapper,
        userService: UserService,
    ) = IdentityProviderController(
        identityProviderService,
        authorizationService,
        principalMapper,
        userService,
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

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun identityProviderService(
        userService: UserService,
        oAuth2ProviderConnectionTokenService: OAuth2ProviderConnectionTokenService,
        hashService: HashService,
        accessTokenCache: AccessTokenCache,
        accessTokenService: AccessTokenService,
        stepUpTokenService: StepUpTokenService,
        emailProperties: EmailProperties,
        securityAlertService: SecurityAlertService,
        securityAlertProperties: SecurityAlertProperties,
        principalService: PrincipalService,
        twoFactorEmailProperties: TwoFactorEmailCodeProperties
    ) = IdentityProviderService(
        userService,
        oAuth2ProviderConnectionTokenService,
        hashService,
        accessTokenCache,
        accessTokenService,
        stepUpTokenService,
        emailProperties,
        securityAlertService,
        securityAlertProperties,
        principalService,
        twoFactorEmailProperties
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
        principalSettingsService: PrincipalSettingsService,
        downloadService: DownloadService,
        identityProviderInfoService: IdentityProviderInfoService,
        emailProperties: EmailProperties
    ) = OAuth2AuthenticationService(
        userService,
        twoFactorEmailCodeProperties,
        identityProviderService,
        accessTokenService,
        principalSettingsService,
        downloadService,
        identityProviderInfoService,
        emailProperties,
    )
}
