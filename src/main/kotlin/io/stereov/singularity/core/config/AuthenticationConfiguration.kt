package io.stereov.singularity.core.config

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.stereov.singularity.core.admin.controller.AdminController
import io.stereov.singularity.core.admin.service.AdminService
import io.stereov.singularity.core.auth.exception.handler.AuthExceptionHandler
import io.stereov.singularity.core.auth.service.AuthenticationService
import io.stereov.singularity.core.auth.service.CookieService
import io.stereov.singularity.core.config.storage.S3Configuration
import io.stereov.singularity.core.global.service.cache.AccessTokenCache
import io.stereov.singularity.core.global.service.cache.RedisService
import io.stereov.singularity.core.global.service.encryption.service.EncryptionService
import io.stereov.singularity.core.global.service.file.service.FileStorage
import io.stereov.singularity.core.global.service.geolocation.GeoLocationService
import io.stereov.singularity.core.global.service.hash.HashService
import io.stereov.singularity.core.global.service.jwt.JwtService
import io.stereov.singularity.core.global.service.jwt.exception.handler.TokenExceptionHandler
import io.stereov.singularity.core.global.service.mail.MailService
import io.stereov.singularity.core.global.service.ratelimit.RateLimitService
import io.stereov.singularity.core.global.service.secrets.service.EncryptionSecretService
import io.stereov.singularity.core.global.service.twofactorauth.TwoFactorAuthService
import io.stereov.singularity.core.properties.*
import io.stereov.singularity.core.user.controller.UserDeviceController
import io.stereov.singularity.core.user.controller.UserSessionController
import io.stereov.singularity.core.user.controller.UserTwoFactorAuthController
import io.stereov.singularity.core.user.repository.UserRepository
import io.stereov.singularity.core.user.service.UserService
import io.stereov.singularity.core.user.service.UserSessionService
import io.stereov.singularity.core.user.service.device.UserDeviceService
import io.stereov.singularity.core.user.service.token.TwoFactorAuthTokenService
import io.stereov.singularity.core.user.service.token.UserTokenService
import io.stereov.singularity.core.user.service.twofactor.UserTwoFactorAuthService
import kotlinx.serialization.json.Json
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.web.reactive.function.client.WebClient

/**
 * # Authentication configuration class.
 *
 * This class is responsible for configuring the authentication-related services
 * and components in the application.
 *
 * It runs after the [MongoReactiveAutoConfiguration], [SpringDataWebAutoConfiguration],
 * [RedisAutoConfiguration], and [ApplicationConfiguration] classes to ensure that
 * the necessary configurations are applied in the correct order.
 *
 * This class enables the following services:
 * - [AuthenticationService]
 * - [GeoLocationService]
 * - [HashService]
 * - [RedisService]
 * - [TwoFactorAuthService]
 * - [TwoFactorAuthTokenService]
 * - [UserService]
 * - [UserSessionService]
 * - [CookieService]
 * - [UserDeviceService]
 * - [UserTokenService]
 * - [UserTwoFactorAuthService]
 *
 * It enabled the following controllers:
 * - [UserSessionController]
 * - [UserTwoFactorAuthController]
 * - [UserDeviceController]
 *
 * It enables the following beans:
 * - [GoogleAuthenticator]
 * - [AccessTokenCache]
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
        S3Configuration::class,
    ]
)
@EnableReactiveMongoRepositories(
    basePackageClasses = [UserRepository::class]
)
@OptIn(ExperimentalLettuceCoroutinesApi::class)
class AuthenticationConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun authenticationService(): AuthenticationService {
        return AuthenticationService()
    }

    @Bean
    @ConditionalOnMissingBean
    fun googleAuthenticator(): GoogleAuthenticator {
        return GoogleAuthenticator()
    }

    // Services

    @Bean
    @ConditionalOnMissingBean
    fun adminService(context: ApplicationContext, userService: UserService, hashService: HashService, appProperties: AppProperties): AdminService {
        return AdminService(context, userService, appProperties, hashService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun cookieService(
        userTokenService: UserTokenService,
        jwtProperties: JwtProperties,
        appProperties: AppProperties,
        geoLocationService: GeoLocationService,
        userService: UserService,
        twoFactorAuthTokenService: TwoFactorAuthTokenService,
        authenticationService: AuthenticationService,
        twoFactorAuthService: TwoFactorAuthService
    ): CookieService {
        return CookieService(userTokenService, jwtProperties, appProperties, geoLocationService, userService, twoFactorAuthTokenService, authenticationService, twoFactorAuthService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun accessTokenCache(
        commands: RedisCoroutinesCommands<String, ByteArray>,
        jwtProperties: JwtProperties,
    ): AccessTokenCache {
        return AccessTokenCache(commands, jwtProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun redisService(redisCoroutinesCommands: RedisCoroutinesCommands<String, ByteArray>): RedisService {
        return RedisService(redisCoroutinesCommands)
    }

    @Bean
    @ConditionalOnMissingBean
    fun geoLocationService(webClient: WebClient): GeoLocationService {
        return GeoLocationService(webClient)
    }

    @Bean
    @ConditionalOnMissingBean
    fun hashService(): HashService {
        return HashService()
    }

    @Bean
    @ConditionalOnMissingBean
    fun rateLimitService(
        authenticationService: AuthenticationService,
        proxyManager: LettuceBasedProxyManager<String>,
        rateLimitProperties: RateLimitProperties,
        loginAttemptLimitProperties: LoginAttemptLimitProperties,
    ): RateLimitService {
        return RateLimitService(authenticationService, proxyManager, rateLimitProperties, loginAttemptLimitProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun userDeviceService(
        userService: UserService,
        authenticationService: AuthenticationService,
    ) : UserDeviceService {
        return UserDeviceService(userService, authenticationService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorAuthService(googleAuthenticator: GoogleAuthenticator): TwoFactorAuthService {
        return TwoFactorAuthService(googleAuthenticator)
    }

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorAuthTokenService(jwtService: JwtService, jwtProperties: JwtProperties, authenticationService: AuthenticationService, twoFactorAuthService: TwoFactorAuthService, hashService: HashService): TwoFactorAuthTokenService {
        return TwoFactorAuthTokenService(jwtService, jwtProperties, authenticationService, twoFactorAuthService, hashService)
    }

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
    fun userService(userRepository: UserRepository, encryptionTransformer: EncryptionService, json: Json, hashService: HashService, encryptionSecretService: EncryptionSecretService): UserService {
        return UserService(userRepository, encryptionTransformer, json, hashService, encryptionSecretService)
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
        fileStorage: FileStorage,
        mailService: MailService,
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
    fun adminController(adminService: AdminService): AdminController {
        return AdminController(adminService)
    }

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
        userService: UserService,
        userSessionService: UserSessionService,
        cookieService: CookieService,
    ): UserSessionController {
        return UserSessionController(authenticationService, userSessionService, cookieService)
    }

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun authExceptionHandler(): AuthExceptionHandler {
        return AuthExceptionHandler()
    }

    @Bean
    @ConditionalOnMissingBean
    fun tokenExceptionHandler(): TokenExceptionHandler {
        return TokenExceptionHandler()
    }
}
