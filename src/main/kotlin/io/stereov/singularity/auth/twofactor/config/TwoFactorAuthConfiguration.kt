package io.stereov.singularity.auth.twofactor.config

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.token.component.CookieCreator
import io.stereov.singularity.auth.token.component.TokenValueExtractor
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.alert.properties.SecurityAlertProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.alert.service.SecurityAlertService
import io.stereov.singularity.auth.token.service.AccessTokenService
import io.stereov.singularity.auth.token.service.RefreshTokenService
import io.stereov.singularity.auth.token.service.SessionTokenService
import io.stereov.singularity.auth.token.service.StepUpTokenService
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.twofactor.controller.EmailAuthenticationController
import io.stereov.singularity.auth.twofactor.controller.TotpAuthenticationController
import io.stereov.singularity.auth.twofactor.controller.TwoFactorAuthenticationController
import io.stereov.singularity.auth.twofactor.exception.handler.TwoFactorAuthExceptionHandler
import io.stereov.singularity.auth.twofactor.properties.TotpRecoveryCodeProperties
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailProperties
import io.stereov.singularity.auth.twofactor.service.EmailAuthenticationService
import io.stereov.singularity.auth.twofactor.service.TotpAuthenticationService
import io.stereov.singularity.auth.twofactor.service.TotpService
import io.stereov.singularity.auth.twofactor.service.TwoFactorAuthenticationService
import io.stereov.singularity.auth.twofactor.service.token.TotpSetupTokenService
import io.stereov.singularity.auth.twofactor.service.token.TwoFactorAuthenticationTokenService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.core.mapper.PrincipalMapper
import io.stereov.singularity.user.core.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.core.ReactiveRedisTemplate

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
        sessionTokenService: SessionTokenService,
    ) = TotpAuthenticationController(
        totpAuthenticationService,
        cookieCreator,
        accessTokenService,
        refreshTokenService,
        stepUpTokenService,
        principalMapper,
        authProperties,
        sessionTokenService,
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
        )
    }

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorAuthExceptionHandler() = TwoFactorAuthExceptionHandler()

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun totpSetupTokenService(
        authorizationService: AuthorizationService,
        jwtService: JwtService,
        jwtProperties: JwtProperties,
    ) = TotpSetupTokenService(authorizationService, jwtService, jwtProperties)
    
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
        redisTemplate: ReactiveRedisTemplate<String, String>,
        emailService: EmailService,
        emailProperties: EmailProperties,
        authorizationService: AuthorizationService,
        accessTokenCache: AccessTokenCache,
        appProperties: AppProperties, securityAlertProperties: SecurityAlertProperties,
        securityAlertService: SecurityAlertService
    ) = EmailAuthenticationService(
        twoFactorEmailCodeProperties,
        userService,
        translateService,
        templateService,
        redisTemplate,
        emailService,
        emailProperties,
        authorizationService,
        accessTokenCache,
        appProperties,
        securityAlertProperties,
        securityAlertService
    )
    
    @Bean
    @ConditionalOnMissingBean
    fun totpAuthenticationService(
        totpService: TotpService,
        authorizationService: AuthorizationService,
        totpRecoveryCodeProperties: TotpRecoveryCodeProperties,
        setupTokenService: TotpSetupTokenService,
        hashService: HashService,
        userService: UserService,
        accessTokenCache: AccessTokenCache,
        principalMapper: PrincipalMapper,
        twoFactorAuthTokenService: TwoFactorAuthenticationTokenService,
        securityAlertProperties: SecurityAlertProperties,
        securityAlertService: SecurityAlertService,
        emailProperties: EmailProperties
    ) = TotpAuthenticationService(
        totpService,
        authorizationService,
        totpRecoveryCodeProperties,
        setupTokenService,
        hashService,
        userService,
        accessTokenCache,
        principalMapper,
        twoFactorAuthTokenService,
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
        twoFactorAuthTokenService: TwoFactorAuthenticationTokenService,
        authorizationService: AuthorizationService,
    ): TwoFactorAuthenticationService {
        return TwoFactorAuthenticationService(
            userService,
            totpService,
            twoFactorAuthTokenService,
            emailAuthenticationService,
            authorizationService,
        )
    }
}
