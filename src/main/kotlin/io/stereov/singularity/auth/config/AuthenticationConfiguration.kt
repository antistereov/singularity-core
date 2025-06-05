package io.stereov.singularity.auth.config

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.stereov.singularity.auth.exception.handler.AuthExceptionHandler
import io.stereov.singularity.auth.properties.AuthProperties
import io.stereov.singularity.auth.service.AuthenticationService
import io.stereov.singularity.auth.service.CookieService
import io.stereov.singularity.config.ApplicationConfiguration
import io.stereov.singularity.config.storage.S3Configuration
import io.stereov.singularity.global.service.cache.AccessTokenCache
import io.stereov.singularity.global.service.cache.RedisService
import io.stereov.singularity.global.service.geolocation.GeoLocationService
import io.stereov.singularity.global.service.jwt.JwtService
import io.stereov.singularity.global.service.jwt.exception.handler.TokenExceptionHandler
import io.stereov.singularity.global.service.twofactorauth.TwoFactorAuthService
import io.stereov.singularity.global.service.twofactorauth.exception.handler.TwoFactorAuthExceptionHandler
import io.stereov.singularity.group.repository.GroupRepository
import io.stereov.singularity.group.service.GroupService
import io.stereov.singularity.hash.HashService
import io.stereov.singularity.properties.AppProperties
import io.stereov.singularity.properties.JwtProperties
import io.stereov.singularity.secrets.service.HashSecretService
import io.stereov.singularity.user.service.UserService
import io.stereov.singularity.user.service.device.UserDeviceService
import io.stereov.singularity.user.service.token.TwoFactorAuthTokenService
import io.stereov.singularity.user.service.token.UserTokenService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
        S3Configuration::class,
    ]
)
@OptIn(ExperimentalLettuceCoroutinesApi::class)
@EnableConfigurationProperties(AuthProperties::class)
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
    fun hashService(hashSecretService: HashSecretService): HashService {
        return HashService(hashSecretService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun groupService(
        groupRepository: GroupRepository,
        appProperties: AppProperties
    ): GroupService {
        return GroupService(groupRepository, appProperties)
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

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorAuthExceptionHandler() = TwoFactorAuthExceptionHandler()
}
