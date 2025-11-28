package io.stereov.singularity.auth.twofactor.config

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.stereov.singularity.auth.alert.properties.SecurityAlertProperties
import io.stereov.singularity.auth.alert.service.SecurityAlertService
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.token.component.CookieCreator
import io.stereov.singularity.auth.token.component.TokenValueExtractor
import io.stereov.singularity.auth.token.service.*
import io.stereov.singularity.auth.twofactor.controller.EmailAuthenticationController
import io.stereov.singularity.auth.twofactor.controller.TotpAuthenticationController
import io.stereov.singularity.auth.twofactor.controller.TwoFactorAuthenticationController
import io.stereov.singularity.auth.twofactor.properties.TotpRecoveryCodeProperties
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailProperties
import io.stereov.singularity.auth.twofactor.service.EmailAuthenticationService
import io.stereov.singularity.auth.twofactor.service.TotpAuthenticationService
import io.stereov.singularity.auth.twofactor.service.TotpService
import io.stereov.singularity.auth.twofactor.service.TwoFactorAuthenticationService
import io.stereov.singularity.cache.service.CacheService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.principal.core.mapper.PrincipalMapper
import io.stereov.singularity.principal.core.service.UserService
import io.stereov.singularity.translate.service.TranslateService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration(
    after = [
        ApplicationConfiguration::class
    ]
)
@EnableConfigurationProperties(
    TotpRecoveryCodeProperties::class,
    TwoFactorEmailCodeProperties::class,
    TwoFactorEmailProperties::class
)
class TwoFactorAuthConfiguration {

    // Controller
    
    @Bean
    @ConditionalOnMissingBean
    fun mailAuthenticationController(
        emailAuthenticationService: EmailAuthenticationService,
        authorizationService: AuthorizationService,
        principalMapper: PrincipalMapper,
        twoFactorAuthTokenService: TwoFactorAuthenticationTokenService,
        userService: UserService
    ) = EmailAuthenticationController(
        emailAuthenticationService,
        authorizationService,
        principalMapper,
        twoFactorAuthTokenService,
        userService
    )
    
    @Bean
    @ConditionalOnMissingBean
    fun totpAuthenticationController(
        totpAuthenticationService: TotpAuthenticationService,
        cookieCreator: CookieCreator,
        accessTokenService: AccessTokenService,
        refreshTokenService: RefreshTokenService,
        stepUpTokenService: StepUpTokenService,
        principalMapper: PrincipalMapper,
        authProperties: AuthProperties,
        authorizationService: AuthorizationService,
        userService: UserService,
        totpSetupTokenService: TotpSetupTokenService,
        twoFactorAuthTokenService: TwoFactorAuthenticationTokenService
    ) = TotpAuthenticationController(
        totpAuthenticationService,
        cookieCreator,
        accessTokenService,
        refreshTokenService,
        stepUpTokenService,
        principalMapper,
        authProperties,
        authorizationService,
        userService,
        totpSetupTokenService,
        twoFactorAuthTokenService,
    )

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorAuthController(
        twoFactorAuthenticationService: TwoFactorAuthenticationService,
        authProperties: AuthProperties,
        geolocationService: GeolocationService,
        principalMapper: PrincipalMapper,
        accessTokenService: AccessTokenService,
        refreshTokenService: RefreshTokenService,
        stepUpTokenService: StepUpTokenService,
        cookieCreator: CookieCreator,
        authorizationService: AuthorizationService,
        twoFactorAuthenticationTokenService: TwoFactorAuthenticationTokenService,
        userService: UserService
    ): TwoFactorAuthenticationController {
        return TwoFactorAuthenticationController(
            twoFactorAuthenticationService,
            authProperties,
            geolocationService,
            principalMapper,
            accessTokenService,
            refreshTokenService,
            stepUpTokenService,
            cookieCreator,
            authorizationService,
            twoFactorAuthenticationTokenService,
            userService
        )
    }

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun totpSetupTokenService(
        jwtService: JwtService,
        jwtProperties: JwtProperties,
    ) = TotpSetupTokenService(
        jwtService,
        jwtProperties)
    
    @Bean
    @ConditionalOnMissingBean
    fun twoFactorAuthenticationTokenService(
        jwtProperties: JwtProperties,
        jwtService: JwtService,
        tokenValueExtractor: TokenValueExtractor
    ) = TwoFactorAuthenticationTokenService(jwtProperties, jwtService, tokenValueExtractor)
    

    @Bean
    @ConditionalOnMissingBean
    fun googleAuthenticator(): GoogleAuthenticator {
        return GoogleAuthenticator()
    }
    
    @Bean
    @ConditionalOnMissingBean
    fun mailAuthenticationService(
        twoFactorEmailCodeProperties: TwoFactorEmailCodeProperties,
        userService: UserService,
        translateService: TranslateService,
        templateService: TemplateService,
        cacheService: CacheService,
        emailService: EmailService,
        emailProperties: EmailProperties,
        accessTokenCache: AccessTokenCache,
        appProperties: AppProperties, securityAlertProperties: SecurityAlertProperties,
        securityAlertService: SecurityAlertService
    ) = EmailAuthenticationService(
        twoFactorEmailCodeProperties,
        userService,
        translateService,
        templateService,
        cacheService,
        emailService,
        emailProperties,
        accessTokenCache,
        appProperties,
        securityAlertProperties,
        securityAlertService
    )
    
    @Bean
    @ConditionalOnMissingBean
    fun totpAuthenticationService(
        totpService: TotpService,
        totpRecoveryCodeProperties: TotpRecoveryCodeProperties,
        setupTokenService: TotpSetupTokenService,
        hashService: HashService,
        userService: UserService,
        accessTokenCache: AccessTokenCache,
        securityAlertProperties: SecurityAlertProperties,
        securityAlertService: SecurityAlertService,
        emailProperties: EmailProperties
    ) = TotpAuthenticationService(
        totpService,
        totpRecoveryCodeProperties,
        setupTokenService,
        hashService,
        userService,
        accessTokenCache,
        securityAlertProperties,
        securityAlertService,
        emailProperties
    )

    @Bean
    @ConditionalOnMissingBean
    fun totpService(googleAuthenticator: GoogleAuthenticator): TotpService {
        return TotpService(googleAuthenticator)
    }
    
    @Bean
    @ConditionalOnMissingBean
    fun userTwoFactorAuthService(
        userService: UserService,
        totpService: TotpAuthenticationService,
        emailAuthenticationService: EmailAuthenticationService,
    ): TwoFactorAuthenticationService {
        return TwoFactorAuthenticationService(
            userService,
            totpService,
            emailAuthenticationService,
        )
    }
}
