package io.stereov.singularity.auth.twofactor.config

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.component.CookieCreator
import io.stereov.singularity.auth.core.component.TokenValueExtractor
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.token.AccessTokenService
import io.stereov.singularity.auth.core.service.token.RefreshTokenService
import io.stereov.singularity.auth.core.service.token.SessionTokenService
import io.stereov.singularity.auth.core.service.token.StepUpTokenService
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.twofactor.controller.MailAuthenticationController
import io.stereov.singularity.auth.twofactor.controller.TotpAuthenticationController
import io.stereov.singularity.auth.twofactor.controller.TwoFactorAuthenticationController
import io.stereov.singularity.auth.twofactor.exception.handler.TwoFactorAuthExceptionHandler
import io.stereov.singularity.auth.twofactor.properties.TwoFactorAuthProperties
import io.stereov.singularity.auth.twofactor.service.MailAuthenticationService
import io.stereov.singularity.auth.twofactor.service.TotpAuthenticationService
import io.stereov.singularity.auth.twofactor.service.TotpService
import io.stereov.singularity.auth.twofactor.service.TwoFactorAuthenticationService
import io.stereov.singularity.auth.twofactor.service.token.TotpSetupTokenService
import io.stereov.singularity.auth.twofactor.service.token.TwoFactorAuthenticationTokenService
import io.stereov.singularity.content.translate.service.TranslateService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.mail.core.properties.MailProperties
import io.stereov.singularity.mail.core.service.MailService
import io.stereov.singularity.mail.template.service.TemplateService
import io.stereov.singularity.user.core.mapper.UserMapper
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
@EnableConfigurationProperties(TwoFactorAuthProperties::class)
class TwoFactorAuthConfiguration {

    // Controller
    
    @Bean
    @ConditionalOnMissingBean
    fun mailAuthenticationController(
        mailAuthenticationService: MailAuthenticationService,
        authorizationService: AuthorizationService
    ) = MailAuthenticationController(mailAuthenticationService, authorizationService)
    
    @Bean
    @ConditionalOnMissingBean
    fun totpAuthenticationController(
        totpAuthenticationService: TotpAuthenticationService,
        cookieCreator: CookieCreator,
        accessTokenService: AccessTokenService,
        refreshTokenService: RefreshTokenService,
        stepUpTokenService: StepUpTokenService,
        userMapper: UserMapper,
        authProperties: AuthProperties,
        sessionTokenService: SessionTokenService
    ) = TotpAuthenticationController(
        totpAuthenticationService,
        cookieCreator,
        accessTokenService,
        refreshTokenService,
        stepUpTokenService,
        userMapper,
        authProperties,
        sessionTokenService
    )

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorAuthController(
        twoFactorAuthenticationService: TwoFactorAuthenticationService,
        authProperties: AuthProperties,
        geolocationService: GeolocationService,
        userMapper: UserMapper,
        accessTokenService: AccessTokenService,
        refreshTokenService: RefreshTokenService,
        stepUpTokenService: StepUpTokenService,
        cookieCreator: CookieCreator,
        authorizationService: AuthorizationService,
        sessionTokenService: SessionTokenService
    ): TwoFactorAuthenticationController {
        return TwoFactorAuthenticationController(
            twoFactorAuthenticationService,
            authProperties,
            geolocationService,
            userMapper,
            accessTokenService,
            refreshTokenService,
            stepUpTokenService,
            cookieCreator,
            authorizationService,
            sessionTokenService,
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
         twoFactorAuthProperties: TwoFactorAuthProperties,
         userService: UserService,
         translateService: TranslateService,
         templateService: TemplateService,
         redisTemplate: ReactiveRedisTemplate<String, String>,
         mailService: MailService,
         mailProperties: MailProperties
    ) = MailAuthenticationService(
        twoFactorAuthProperties,
        userService,
        translateService,
        templateService,
        redisTemplate,
        mailService,
        mailProperties
    )
    
    @Bean
    @ConditionalOnMissingBean
    fun totpAuthenticationService(
        totpService: TotpService,
        authorizationService: AuthorizationService,
        twoFactorAuthProperties: TwoFactorAuthProperties,
        setupTokenService: TotpSetupTokenService,
        hashService: HashService,
        userService: UserService,
        accessTokenCache: AccessTokenCache,
        userMapper: UserMapper,
        twoFactorAuthTokenService: TwoFactorAuthenticationTokenService
    ) = TotpAuthenticationService(
        totpService,
        authorizationService,
        twoFactorAuthProperties,
        setupTokenService,
        hashService,
        userService,
        accessTokenCache,
        userMapper,
        twoFactorAuthTokenService
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
        mailAuthenticationService: MailAuthenticationService,
        twoFactorAuthTokenService: TwoFactorAuthenticationTokenService
    ): TwoFactorAuthenticationService {
        return TwoFactorAuthenticationService(
            userService,
            totpService,
            twoFactorAuthTokenService,
            mailAuthenticationService,
        )
    }
}
