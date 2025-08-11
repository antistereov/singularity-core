package io.stereov.singularity.user.twofactor.config

import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.auth.core.service.CookieService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.auth.twofactor.properties.TwoFactorAuthProperties
import io.stereov.singularity.auth.twofactor.service.TwoFactorAuthService
import io.stereov.singularity.user.core.config.UserConfiguration
import io.stereov.singularity.user.core.service.UserService
import io.stereov.singularity.user.token.cache.AccessTokenCache
import io.stereov.singularity.user.token.service.TwoFactorTokenService
import io.stereov.singularity.user.twofactor.controller.UserTwoFactorAuthController
import io.stereov.singularity.user.twofactor.service.UserTwoFactorAuthService
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
class UserTwoFactorConfiguration {

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun userTwoFactorAuthService(
        userService: UserService,
        twoFactorAuthService: TwoFactorAuthService,
        authenticationService: AuthenticationService,
        twoFactorAuthProperties: TwoFactorAuthProperties,
        hashService: HashService,
        cookieService: CookieService,
        twoFactorTokenService: TwoFactorTokenService,
        accessTokenCache: AccessTokenCache,
    ): UserTwoFactorAuthService {
        return UserTwoFactorAuthService(userService, twoFactorAuthService, authenticationService, twoFactorAuthProperties, hashService, cookieService, twoFactorTokenService, accessTokenCache)
    }

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun userTwoFactorAuthController(
        userTwoFactorAuthService: UserTwoFactorAuthService,
        cookieService: CookieService,
        authenticationService: AuthenticationService,
        userService: UserService
    ): UserTwoFactorAuthController {
        return UserTwoFactorAuthController(
            userTwoFactorAuthService,
            cookieService,
            authenticationService,
            userService
        )
    }
}
