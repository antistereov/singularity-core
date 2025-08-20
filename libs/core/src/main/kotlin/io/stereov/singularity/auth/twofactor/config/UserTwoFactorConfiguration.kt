package io.stereov.singularity.auth.twofactor.config

import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.auth.core.service.CookieService
import io.stereov.singularity.auth.token.cache.AccessTokenCache
import io.stereov.singularity.auth.token.service.TwoFactorTokenService
import io.stereov.singularity.auth.twofactor.controller.UserTwoFactorAuthController
import io.stereov.singularity.auth.twofactor.properties.TwoFactorAuthProperties
import io.stereov.singularity.auth.twofactor.service.TwoFactorAuthService
import io.stereov.singularity.auth.twofactor.service.UserTwoFactorAuthService
import io.stereov.singularity.database.hash.service.HashService
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
        userMapper: UserMapper
    ): UserTwoFactorAuthService {
        return UserTwoFactorAuthService(
            userService,
            twoFactorAuthService,
            authenticationService,
            twoFactorAuthProperties,
            hashService,
            cookieService,
            twoFactorTokenService,
            accessTokenCache,
            userMapper
        )
    }

    // Controller

    @Bean
    @ConditionalOnMissingBean
    fun userTwoFactorAuthController(
        userTwoFactorAuthService: UserTwoFactorAuthService,
        cookieService: CookieService,
        authenticationService: AuthenticationService,
        userMapper: UserMapper
    ): UserTwoFactorAuthController {
        return UserTwoFactorAuthController(
            userTwoFactorAuthService,
            cookieService,
            authenticationService,
            userMapper
        )
    }
}
