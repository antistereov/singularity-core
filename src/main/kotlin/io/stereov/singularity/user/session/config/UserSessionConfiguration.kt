package io.stereov.singularity.user.session.config

import io.stereov.singularity.auth.properties.AuthProperties
import io.stereov.singularity.auth.service.AuthenticationService
import io.stereov.singularity.auth.service.CookieService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.user.core.config.UserConfiguration
import io.stereov.singularity.user.core.service.UserService
import io.stereov.singularity.user.device.service.UserDeviceService
import io.stereov.singularity.user.mail.service.UserMailSender
import io.stereov.singularity.user.session.controller.UserSessionController
import io.stereov.singularity.user.session.service.UserSessionService
import io.stereov.singularity.user.token.cache.AccessTokenCache
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
        userService: UserService,
        authProperties: AuthProperties
    ): UserSessionController {
        return UserSessionController(
            authenticationService,
            userSessionService,
            cookieService,
            userService,
            authProperties
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
        cookieService: CookieService,
        fileStorage: FileStorage,
        mailService: UserMailSender,
        appProperties: AppProperties
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
            appProperties
        )
    }
}
