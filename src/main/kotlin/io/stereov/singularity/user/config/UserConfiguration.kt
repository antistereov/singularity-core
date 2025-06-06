package io.stereov.singularity.user.config

import io.stereov.singularity.auth.config.AuthenticationConfiguration
import io.stereov.singularity.auth.service.AuthenticationService
import io.stereov.singularity.auth.service.CookieService
import io.stereov.singularity.cache.service.AccessTokenCache
import io.stereov.singularity.encryption.service.EncryptionService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.hash.service.HashService
import io.stereov.singularity.jwt.properties.JwtProperties
import io.stereov.singularity.jwt.service.JwtService
import io.stereov.singularity.mail.config.MailConfiguration
import io.stereov.singularity.mail.properties.MailProperties
import io.stereov.singularity.mail.service.MailService
import io.stereov.singularity.encryption.service.EncryptionSecretService
import io.stereov.singularity.template.service.TemplateService
import io.stereov.singularity.translate.service.TranslateService
import io.stereov.singularity.twofactorauth.properties.TwoFactorAuthProperties
import io.stereov.singularity.twofactorauth.service.TwoFactorAuthService
import io.stereov.singularity.user.controller.UserDeviceController
import io.stereov.singularity.user.controller.UserSessionController
import io.stereov.singularity.user.controller.UserTwoFactorAuthController
import io.stereov.singularity.user.repository.UserRepository
import io.stereov.singularity.user.service.UserService
import io.stereov.singularity.user.service.UserSessionService
import io.stereov.singularity.user.service.device.UserDeviceService
import io.stereov.singularity.user.service.mail.MailCooldownService
import io.stereov.singularity.user.service.mail.MailTokenService
import io.stereov.singularity.user.service.mail.UserMailSender
import io.stereov.singularity.user.service.mail.UserMailService
import io.stereov.singularity.user.service.token.TwoFactorAuthTokenService
import io.stereov.singularity.user.service.token.UserTokenService
import io.stereov.singularity.user.service.twofactor.UserTwoFactorAuthService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.data.redis.core.ReactiveRedisTemplate

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
        AuthenticationConfiguration::class,
        MailConfiguration:: class,
    ]
)
@EnableReactiveMongoRepositories(basePackageClasses = [UserRepository::class])
class UserConfiguration {



    // Service

    @Bean
    @ConditionalOnMissingBean
    fun userDeviceService(
        userService: UserService,
        authenticationService: AuthenticationService
    ) = UserDeviceService(userService, authenticationService)

    @Bean
    @ConditionalOnMissingBean
    fun mailCooldownService(
        redisTemplate: ReactiveRedisTemplate<String, String>,
        mailProperties: MailProperties
    ): MailCooldownService {
        return MailCooldownService(redisTemplate, mailProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun mailTokenService(
        mailProperties: MailProperties,
        jwtService: JwtService,
        encryptionService: EncryptionService
    ): MailTokenService {
        return MailTokenService(mailProperties, jwtService, encryptionService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun userMailService(
        userService: UserService,
        authenticationService: AuthenticationService,
        mailCooldownService: MailCooldownService,
        mailService: UserMailSender,
        mailTokenService: MailTokenService,
        hashService: HashService
    ) = UserMailService(userService, authenticationService, mailCooldownService, mailService, mailTokenService, hashService)

    @Bean
    @ConditionalOnMissingBean
    fun userMailSender(
        mailCooldownService: MailCooldownService,
        mailTokenService: MailTokenService,
        uiProperties: UiProperties,
        translateService: TranslateService,
        mailService: MailService,
        templateService: TemplateService
    ) = UserMailSender(mailCooldownService, mailTokenService, uiProperties, translateService, mailService, templateService)

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorAuthTokenService(
        jwtService: JwtService,
        jwtProperties: JwtProperties,
        authenticationService: AuthenticationService,
        twoFactorAuthService: TwoFactorAuthService,
        hashService: HashService
    ) = TwoFactorAuthTokenService(jwtService, jwtProperties, authenticationService, twoFactorAuthService, hashService)

    @Bean
    @ConditionalOnMissingBean
    fun userTokenService(
        jwtService: JwtService,
        accessTokenCache: AccessTokenCache,
        jwtProperties: JwtProperties,
    ): UserTokenService {
        return UserTokenService(jwtService, accessTokenCache, jwtProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun userTwoFactorAuthService(
        userService: UserService,
        twoFactorAuthService: TwoFactorAuthService,
        authenticationService: AuthenticationService,
        twoFactorAuthProperties: TwoFactorAuthProperties,
        hashService: HashService,
        cookieService: CookieService,
        twoFactorAuthTokenService: TwoFactorAuthTokenService,
        accessTokenCache: AccessTokenCache,
    ): UserTwoFactorAuthService {
        return UserTwoFactorAuthService(userService, twoFactorAuthService, authenticationService, twoFactorAuthProperties, hashService, cookieService, twoFactorAuthTokenService, accessTokenCache)
    }

    @Bean
    @ConditionalOnMissingBean
    fun userService(userRepository: UserRepository, encryptionTransformer: EncryptionService, hashService: HashService, encryptionSecretService: EncryptionSecretService): UserService {
        return UserService(userRepository, encryptionTransformer, hashService, encryptionSecretService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun userSessionService(
        userService: UserService,
        hashService: HashService,
        authenticationService: AuthenticationService,
        deviceService: UserDeviceService,
        accessTokenCache: AccessTokenCache,
        cookieService: CookieService,
        fileStorage: io.stereov.singularity.file.service.FileStorage,
        mailService: UserMailSender,
    ): UserSessionService {
        return UserSessionService(
            userService,
            hashService,
            authenticationService,
            deviceService,
            accessTokenCache,
            cookieService,
            fileStorage,
            mailService,
        )
    }

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun userTwoFactorAuthController(
        userTwoFactorAuthService: UserTwoFactorAuthService,
        cookieService: CookieService,
        authenticationService: AuthenticationService
    ): UserTwoFactorAuthController {
        return UserTwoFactorAuthController(userTwoFactorAuthService, cookieService, authenticationService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun userDeviceController(
        userDeviceService: UserDeviceService
    ): UserDeviceController {
        return UserDeviceController(userDeviceService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun userSessionController(
        authenticationService: AuthenticationService,
        userSessionService: UserSessionService,
        cookieService: CookieService,
    ): UserSessionController {
        return UserSessionController(authenticationService, userSessionService, cookieService)
    }

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun userExceptionHandler() = io.stereov.singularity.user.exception.handler.UserExceptionHandler()
}
