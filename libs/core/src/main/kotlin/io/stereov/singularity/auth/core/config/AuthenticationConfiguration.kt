package io.stereov.singularity.auth.core.config

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.component.TokenValueExtractor
import io.stereov.singularity.auth.core.controller.AuthenticationController
import io.stereov.singularity.auth.core.controller.EmailVerificationController
import io.stereov.singularity.auth.core.controller.PasswordResetController
import io.stereov.singularity.auth.core.controller.SessionController
import io.stereov.singularity.auth.core.exception.handler.AuthExceptionHandler
import io.stereov.singularity.auth.core.mapper.SessionMapper
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.properties.EmailVerificationProperties
import io.stereov.singularity.auth.core.properties.PasswordResetProperties
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
    EmailVerificationProperties::class,
    PasswordResetProperties::class
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
            authorizationService
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
            twoFactorEmailProperties
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
        authorizationService: AuthorizationService,
        emailVerificationTokenService: EmailVerificationTokenService,
        userMapper: UserMapper,
        redisTemplate: ReactiveRedisTemplate<String, String>,
        emailProperties: EmailProperties,
        emailVerificationProperties: EmailVerificationProperties,
        translateService: TranslateService,
        emailService: EmailService,
        templateService: TemplateService,
        appProperties: AppProperties
    ) = EmailVerificationService(
        userService,
        authorizationService,
        emailVerificationTokenService,
        userMapper,
        redisTemplate,
        emailProperties,
        emailVerificationProperties,
        translateService,
        emailService,
        templateService,
        appProperties
    )
    
    @Bean
    @ConditionalOnMissingBean
    fun passwordResetService(
        userService: UserService,
        passwordResetTokenService: PasswordResetTokenService,
        hashService: HashService,
        redisTemplate: ReactiveRedisTemplate<String, String>,
        emailProperties: EmailProperties,
        passwordResetProperties: PasswordResetProperties,
        translateService: TranslateService,
        emailService: EmailService,
        templateService: TemplateService,
        accessTokenCache: AccessTokenCache,
        appProperties: AppProperties
    ) = PasswordResetService(
        userService,
        passwordResetTokenService,
        hashService,
        redisTemplate,
        emailProperties,
        passwordResetProperties,
        translateService,
        emailService,
        templateService,
        accessTokenCache,
        appProperties
    )

    @Bean
    @ConditionalOnMissingBean
    fun sessionService(
        userService: UserService,
        authorizationService: AuthorizationService,
        accessTokenCache: AccessTokenCache,
    ) = SessionService(userService, authorizationService, accessTokenCache)

}
