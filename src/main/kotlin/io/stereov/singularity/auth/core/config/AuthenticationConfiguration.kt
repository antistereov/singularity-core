package io.stereov.singularity.auth.core.config

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.component.ProviderStringCreator
import io.stereov.singularity.auth.core.component.TokenValueExtractor
import io.stereov.singularity.auth.core.controller.AuthenticationController
import io.stereov.singularity.auth.core.controller.EmailVerificationController
import io.stereov.singularity.auth.core.controller.PasswordResetController
import io.stereov.singularity.auth.core.controller.SessionController
import io.stereov.singularity.auth.core.exception.handler.AuthExceptionHandler
import io.stereov.singularity.auth.core.mapper.SessionMapper
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.properties.SecurityAlertProperties
import io.stereov.singularity.auth.core.service.*
import io.stereov.singularity.auth.core.service.token.*
import io.stereov.singularity.auth.geolocation.properties.GeolocationProperties
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailProperties
import io.stereov.singularity.auth.twofactor.service.TwoFactorAuthenticationService
import io.stereov.singularity.auth.twofactor.service.token.TwoFactorAuthenticationTokenService
import io.stereov.singularity.cache.service.CacheService
import io.stereov.singularity.database.encryption.service.EncryptionService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.email.core.service.EmailService
import io.stereov.singularity.email.template.service.TemplateService
import io.stereov.singularity.file.s3.config.S3Configuration
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.ReactiveRedisTemplate

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
        S3Configuration::class,
    ]
)
@EnableConfigurationProperties(
    AuthProperties::class,
    SecurityAlertProperties::class
)
class AuthenticationConfiguration {

    // Cache

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    @Bean
    @ConditionalOnMissingBean
    fun accessTokenCache(
        cacheService: CacheService,
        jwtProperties: JwtProperties
    ) = AccessTokenCache(cacheService, jwtProperties)


    // Component

    @Bean
    @ConditionalOnMissingBean
    fun cookieCreator(appProperties: AppProperties) = CookieCreator(appProperties)
    
    @Bean
    @ConditionalOnMissingBean
    fun providerStringCreator(translateService: TranslateService) = ProviderStringCreator(translateService)


    @Bean
    @ConditionalOnMissingBean
    fun tokenValueExtractor(authProperties: AuthProperties) = TokenValueExtractor(authProperties)

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun authenticationController(
        authenticationService: AuthenticationService,
        userMapper: UserMapper,
        authProperties: AuthProperties,
        geoLocationService: GeolocationService,
        twoFactorAuthenticationTokenService: TwoFactorAuthenticationTokenService,
        cookieCreator: CookieCreator,
        accessTokenService: AccessTokenService,
        refreshTokenService: RefreshTokenService,
        userService: UserService,
        stepUpTokenService: StepUpTokenService,
        twoFactorAuthenticationService: TwoFactorAuthenticationService,
        authorizationService: AuthorizationService,
        loginAlertService: LoginAlertService,
        securityAlertProperties: SecurityAlertProperties,
        emailProperties: EmailProperties,

        ): AuthenticationController {
        return AuthenticationController(
            authenticationService,
            userMapper,
            authProperties,
            geoLocationService,
            twoFactorAuthenticationTokenService,
            cookieCreator,
            accessTokenService,
            refreshTokenService,
            userService,
            stepUpTokenService,
            twoFactorAuthenticationService,
            authorizationService,
            loginAlertService,
            securityAlertProperties,
            emailProperties
        )
    }
    
    @Bean
    @ConditionalOnMissingBean
    fun emailVerificationController(
        emailVerificationService: EmailVerificationService
    ) = EmailVerificationController(emailVerificationService)
    
    @Bean
    @ConditionalOnMissingBean
    fun passwordResetController(
        passwordResetService: PasswordResetService
    ) = PasswordResetController(passwordResetService)

    @Bean
    @ConditionalOnMissingBean
    fun sessionController(
        sessionService: SessionService,
        cookieCreator: CookieCreator,
        sessionTokenService: SessionTokenService,
        sessionMapper: SessionMapper
    ): SessionController {
        return SessionController(
            sessionService,
            cookieCreator,
            sessionTokenService,
            sessionMapper,
        )
    }

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun authExceptionHandler(): AuthExceptionHandler {
        return AuthExceptionHandler()
    }

    // Mapper

    @Bean
    @ConditionalOnMissingBean
    fun sessionMapper() = SessionMapper()

    // Services

    @Bean
    @ConditionalOnMissingBean
    fun accessTokenService(
        jwtService: JwtService,
        accessTokenCache: AccessTokenCache,
        jwtProperties: JwtProperties,
        tokenValueExtractor: TokenValueExtractor,
    ) = AccessTokenService(
        jwtService,
        accessTokenCache,
        jwtProperties,
        tokenValueExtractor,
    )

    @Bean
    @ConditionalOnMissingBean
    fun emailVerificationTokenService(
        jwtProperties: JwtProperties,
        jwtService: JwtService
    ) = EmailVerificationTokenService(
        jwtProperties,
        jwtService
    )

    @Bean
    @ConditionalOnMissingBean
    fun passwordResetTokenService(
        jwtProperties: JwtProperties,
        jwtService: JwtService,
        encryptionService: EncryptionService,
    ) = PasswordResetTokenService(
        jwtProperties, jwtService, encryptionService
    )

    @Bean
    @ConditionalOnMissingBean
    fun refreshTokenService(
        jwtService: JwtService,
        jwtProperties: JwtProperties,
        geolocationService: GeolocationService,
        geolocationProperties: GeolocationProperties,
        userService: UserService,
        tokenValueExtractor: TokenValueExtractor,
    ) = RefreshTokenService(
        jwtService,
        jwtProperties,
        geolocationService,
        geolocationProperties,
        userService,
        tokenValueExtractor,
    )

    @Bean
    @ConditionalOnMissingBean
    fun sessionTokenService(
        jwtService: JwtService,
        jwtProperties: JwtProperties,
        tokenValueExtractor: TokenValueExtractor
    ) = SessionTokenService(
        jwtService,
        jwtProperties,
        tokenValueExtractor
    )
    
    @Bean
    @ConditionalOnMissingBean
    fun stepUpTokenService(
        jwtService: JwtService,
        jwtProperties: JwtProperties,
        tokenValueExtractor: TokenValueExtractor,
    ): StepUpTokenService {
        return StepUpTokenService(
            jwtService,
            jwtProperties,
            tokenValueExtractor
        )
    }

    @Bean
    @ConditionalOnMissingBean
    fun authenticationService(
        userService: UserService,
        hashService: HashService,
        authorizationService: AuthorizationService,
        sessionService: SessionService,
        accessTokenCache: AccessTokenCache,
        emailVerificationService: EmailVerificationService,
        twoFactorEmailCodeProperties: TwoFactorEmailCodeProperties,
        emailProperties: EmailProperties,
        twoFactorEmailProperties: TwoFactorEmailProperties,
        registrationAlertService: RegistrationAlertService,
        securityAlertProperties: SecurityAlertProperties,
        identityProviderInfoService: IdentityProviderInfoService
    ): AuthenticationService {
        return AuthenticationService(
            userService,
            hashService,
            authorizationService,
            sessionService,
            accessTokenCache,
            emailVerificationService,
            twoFactorEmailCodeProperties,
            emailProperties,
            twoFactorEmailProperties,
            registrationAlertService,
            securityAlertProperties,
            identityProviderInfoService,
        )
    }

    @Bean
    @ConditionalOnMissingBean
    fun authorizationService(
        stepUpTokenService: StepUpTokenService,
        userService: UserService
    ): AuthorizationService {
        return AuthorizationService(stepUpTokenService, userService)
    }
    
    @Bean
    @ConditionalOnMissingBean
    fun emailVerificationService(
        userService: UserService,
        emailVerificationTokenService: EmailVerificationTokenService,
        userMapper: UserMapper,
        redisTemplate: ReactiveRedisTemplate<String, String>,
        emailProperties: EmailProperties,
        uiProperties: UiProperties,
        translateService: TranslateService,
        emailService: EmailService,
        templateService: TemplateService,
        appProperties: AppProperties,
        securityAlertProperties: SecurityAlertProperties,
        securityAlertService: SecurityAlertService,
        noAccountInfoService: NoAccountInfoService
    ) = EmailVerificationService(
        userService,
        emailVerificationTokenService,
        userMapper,
        redisTemplate,
        emailProperties,
        uiProperties,
        translateService,
        emailService,
        templateService,
        appProperties,
        securityAlertService,
        securityAlertProperties,
        noAccountInfoService,
    )
    
    @Bean
    @ConditionalOnMissingBean
    fun identityProviderInfoService(
        appProperties: AppProperties,
        translateService: TranslateService,
        emailService: EmailService,
        templateService: TemplateService,
        providerStringCreator: ProviderStringCreator,
        redisTemplate: ReactiveRedisTemplate<String, String>,
        emailProperties: EmailProperties,
        passwordResetService: PasswordResetService
    ) = IdentityProviderInfoService(
        appProperties,
        translateService,
        emailService,
        templateService,
        providerStringCreator,
        redisTemplate,
        emailProperties,
        passwordResetService
    )

    @Bean
    @ConditionalOnMissingBean
    fun loginAlertService(
        appProperties: AppProperties,
        translateService: TranslateService,
        emailService: EmailService,
        templateService: TemplateService
    ) = LoginAlertService(
        appProperties,
        translateService,
        emailService,
        templateService
    )

    @Bean
    @ConditionalOnMissingBean
    fun noAccountInfoService(
        appProperties: AppProperties,
        translateService: TranslateService,
        emailService: EmailService,
        templateService: TemplateService,
        redisTemplate: ReactiveRedisTemplate<String, String>,
        emailProperties: EmailProperties
    ) = NoAccountInfoService(
        appProperties,
        translateService,
        emailService,
        templateService,
        redisTemplate,
        emailProperties
    )
    
    @Bean
    @ConditionalOnMissingBean
    fun passwordResetService(
        userService: UserService,
        passwordResetTokenService: PasswordResetTokenService,
        hashService: HashService,
        redisTemplate: ReactiveRedisTemplate<String, String>,
        emailProperties: EmailProperties,
        uiProperties: UiProperties,
        translateService: TranslateService,
        emailService: EmailService,
        templateService: TemplateService,
        accessTokenCache: AccessTokenCache,
        appProperties: AppProperties,
        securityAlertService: SecurityAlertService,
        securityAlertProperties: SecurityAlertProperties,
        noAccountInfoService: NoAccountInfoService
    ) = PasswordResetService(
        userService,
        passwordResetTokenService,
        hashService,
        redisTemplate,
        emailProperties,
        uiProperties,
        translateService,
        emailService,
        templateService,
        accessTokenCache,
        appProperties,
        securityAlertService,
        securityAlertProperties,
        noAccountInfoService
    )

    @Bean
    @ConditionalOnMissingBean
    fun registrationAlertService(
        translateService: TranslateService,
        templateService: TemplateService,
        appProperties: AppProperties,
        emailService: EmailService,
        providerStringCreator: ProviderStringCreator
    ) = RegistrationAlertService(
        translateService,
        templateService,
        appProperties,
        emailService,
        providerStringCreator
    )

    @Bean
    @ConditionalOnMissingBean
    fun securityAlertService(
        appProperties: AppProperties,
        translateService: TranslateService,
        emailService: EmailService,
        templateService: TemplateService
    ) = SecurityAlertService(
        appProperties,
        translateService,
        emailService,
        templateService
    )

    @Bean
    @ConditionalOnMissingBean
    fun sessionService(
        userService: UserService,
        authorizationService: AuthorizationService,
        accessTokenCache: AccessTokenCache,
    ) = SessionService(userService, authorizationService, accessTokenCache)

}
