package io.stereov.web.config

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.stereov.web.auth.exception.handler.AuthExceptionHandler
import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.auth.service.CookieService
import io.stereov.web.global.service.cache.AccessTokenCache
import io.stereov.web.global.service.cache.RedisService
import io.stereov.web.global.service.encryption.EncryptionService
import io.stereov.web.global.service.geolocation.GeoLocationService
import io.stereov.web.global.service.hash.HashService
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.global.service.jwt.exception.handler.TokenExceptionHandler
import io.stereov.web.global.service.ratelimit.RateLimitService
import io.stereov.web.global.service.twofactorauth.TwoFactorAuthService
import io.stereov.web.properties.*
import io.stereov.web.user.controller.UserDeviceController
import io.stereov.web.user.controller.UserSessionController
import io.stereov.web.user.controller.UserTwoFactorAuthController
import io.stereov.web.user.repository.UserRepository
import io.stereov.web.user.service.UserService
import io.stereov.web.user.service.UserSessionService
import io.stereov.web.user.service.device.UserDeviceService
import io.stereov.web.user.service.token.TwoFactorAuthTokenService
import io.stereov.web.user.service.token.UserTokenService
import io.stereov.web.user.service.twofactor.UserTwoFactorAuthService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
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
        WebClientConfiguration::class,
        RedisConfiguration::class,
        JwtConfiguration::class,
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
    fun cookieService(
        userTokenService: UserTokenService,
        jwtProperties: JwtProperties,
        appProperties: AppProperties,
        geoLocationService: GeoLocationService,
        userService: UserService,
        twoFactorAuthTokenService: TwoFactorAuthTokenService
    ): CookieService {
        return CookieService(userTokenService, jwtProperties, appProperties, geoLocationService, userService, twoFactorAuthTokenService)
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
    fun userTokenService(
        jwtService: JwtService,
        accessTokenCache: AccessTokenCache,
        jwtProperties: JwtProperties
    ): UserTokenService {
        return UserTokenService(jwtService, accessTokenCache, jwtProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun userTwoFactorAuthService(
        userService: UserService,
        twoFactorAuthService: TwoFactorAuthService,
        encryptionService: EncryptionService,
        authenticationService: AuthenticationService,
        twoFactorAuthProperties: TwoFactorAuthProperties,
        hashService: HashService,
        cookieService: CookieService
    ): UserTwoFactorAuthService {
        return UserTwoFactorAuthService(userService, twoFactorAuthService, encryptionService, authenticationService, twoFactorAuthProperties, hashService, cookieService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun userService(userRepository: UserRepository): UserService {
        return UserService(userRepository)
    }

    @Bean
    @ConditionalOnMissingBean
    fun userSessionService(
        userService: UserService,
        hashService: HashService,
        jwtService: JwtService,
        authenticationService: AuthenticationService,
        deviceService: UserDeviceService,
        userTwoFactorAuthService: UserTwoFactorAuthService,
        accessTokenCache: AccessTokenCache,
    ): UserSessionService {
        return UserSessionService(
            userService,
            hashService,
            authenticationService,
            deviceService,
            userTwoFactorAuthService,
            accessTokenCache
        )
    }

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun userTwoFactorAuthController(
        userTwoFactorAuthService: UserTwoFactorAuthService,
        cookieService: CookieService
    ): UserTwoFactorAuthController {
        return UserTwoFactorAuthController(userTwoFactorAuthService, cookieService)
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
