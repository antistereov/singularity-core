package io.stereov.singularity.auth.session.config

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.auth.core.service.CookieCreator
import io.stereov.singularity.auth.core.service.TokenValueExtractor
import io.stereov.singularity.auth.device.service.UserDeviceService
import io.stereov.singularity.auth.geolocation.properties.GeolocationProperties
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.session.cache.AccessTokenCache
import io.stereov.singularity.auth.session.controller.UserSessionController
import io.stereov.singularity.auth.session.service.AccessTokenService
import io.stereov.singularity.auth.session.service.RefreshTokenService
import io.stereov.singularity.auth.session.service.UserSessionService
import io.stereov.singularity.auth.twofactor.service.TwoFactorLoginTokenService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.mail.user.service.UserMailSender
import io.stereov.singularity.user.core.config.UserConfiguration
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        UserConfiguration::class
    ]
)
class UserSessionConfiguration {

    // Cache

    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    @Bean
    @ConditionalOnMissingBean
    fun accessTokenCache(
        commands: RedisCoroutinesCommands<String, ByteArray>,
        jwtProperties: JwtProperties
    ) = AccessTokenCache(commands, jwtProperties)

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun userSessionController(
        authenticationService: AuthenticationService,
        userSessionService: UserSessionService,
        userMapper: UserMapper,
        authProperties: AuthProperties,
        geoLocationService: GeolocationService,
        loginTokenService: TwoFactorLoginTokenService,
        cookieCreator: CookieCreator,
        accessTokenService: AccessTokenService,
        refreshTokenService: RefreshTokenService,
        userService: UserService
    ): UserSessionController {
        return UserSessionController(
            authenticationService,
            userSessionService,
            userMapper,
            authProperties,
            geoLocationService,
            loginTokenService,
            cookieCreator,
            accessTokenService,
            refreshTokenService,
            userService
        )
    }

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun accessTokenService(
        jwtService: JwtService,
        accessTokenCache: AccessTokenCache,
        jwtProperties: JwtProperties,
        tokenValueExtractor: TokenValueExtractor
    ) = AccessTokenService(jwtService, accessTokenCache, jwtProperties, tokenValueExtractor)

    @Bean
    @ConditionalOnMissingBean
    fun refreshTokenService(
        jwtService: JwtService,
        jwtProperties: JwtProperties,
        geolocationService: GeolocationService,
        geolocationProperties: GeolocationProperties,
        userService: UserService,
        tokenValueExtractor: TokenValueExtractor
    ) = RefreshTokenService(jwtService, jwtProperties, geolocationService, geolocationProperties, userService, tokenValueExtractor)

    @Bean
    @ConditionalOnMissingBean
    fun userSessionService(
        userService: UserService,
        hashService: HashService,
        authenticationService: AuthenticationService,
        deviceService: UserDeviceService,
        accessTokenCache: AccessTokenCache,
        mailService: UserMailSender,
        appProperties: AppProperties
    ): UserSessionService {
        return UserSessionService(
            userService,
            hashService,
            authenticationService,
            deviceService,
            accessTokenCache,
            mailService,
            appProperties
        )
    }
}
