package io.stereov.singularity.auth.session.config

import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.auth.core.service.CookieService
import io.stereov.singularity.auth.device.service.UserDeviceService
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.session.controller.UserSessionController
import io.stereov.singularity.auth.session.service.UserSessionService
import io.stereov.singularity.auth.token.cache.AccessTokenCache
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

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun userSessionController(
        authenticationService: AuthenticationService,
        userSessionService: UserSessionService,
        cookieService: CookieService,
        userMapper: UserMapper,
        authProperties: AuthProperties,
        geoLocationService: GeolocationService
    ): UserSessionController {
        return UserSessionController(
            authenticationService,
            userSessionService,
            cookieService,
            userMapper,
            authProperties,
            geoLocationService
        )
    }

    // Service

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
