package io.stereov.singularity.auth.twofactor.config

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.auth.core.service.CookieCreator
import io.stereov.singularity.auth.core.service.TokenValueExtractor
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.session.cache.AccessTokenCache
import io.stereov.singularity.auth.session.service.AccessTokenService
import io.stereov.singularity.auth.session.service.RefreshTokenService
import io.stereov.singularity.auth.twofactor.controller.UserTwoFactorAuthController
import io.stereov.singularity.auth.twofactor.exception.handler.TwoFactorAuthExceptionHandler
import io.stereov.singularity.auth.twofactor.properties.TwoFactorAuthProperties
import io.stereov.singularity.auth.twofactor.service.*
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

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
    fun userTwoFactorAuthController(
        userTwoFactorAuthService: UserTwoFactorAuthService,
        authProperties: AuthProperties,
        geolocationService: GeolocationService,
        userMapper: UserMapper,
        accessTokenService: AccessTokenService,
        refreshTokenService: RefreshTokenService,
        setupInitTokenService: TwoFactorInitSetupTokenService,
        stepUpTokenService: StepUpTokenService,
        cookieCreator: CookieCreator
    ): UserTwoFactorAuthController {
        return UserTwoFactorAuthController(
            userTwoFactorAuthService,
            authProperties,
            geolocationService,
            userMapper,
            accessTokenService,
            refreshTokenService,
            setupInitTokenService,
            stepUpTokenService,
            cookieCreator
        )
    }

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorAuthExceptionHandler() = TwoFactorAuthExceptionHandler()

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun googleAuthenticator(): GoogleAuthenticator {
        return GoogleAuthenticator()
    }

    @Bean
    @ConditionalOnMissingBean
    fun stepUpTokenService(
        jwtService: JwtService,
        jwtProperties: JwtProperties,
        authenticationService: AuthenticationService,
        twoFactorAuthService: TwoFactorAuthService,
        tokenValueExtractor: TokenValueExtractor,
    ): StepUpTokenService {
        return StepUpTokenService(jwtService, jwtProperties, authenticationService, twoFactorAuthService, tokenValueExtractor)
    }

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorAuthService(googleAuthenticator: GoogleAuthenticator): TwoFactorAuthService {
        return TwoFactorAuthService(googleAuthenticator)
    }

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorLoginTokenService(
        jwtProperties: JwtProperties,
        jwtService: JwtService,
        tokenValueExtractor: TokenValueExtractor
    ) = TwoFactorLoginTokenService(jwtProperties, jwtService, tokenValueExtractor)

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorSetupInitTokenService(
        authenticationService: AuthenticationService,
        hashService: HashService,
        jwtService: JwtService,
        jwtProperties: JwtProperties,
        tokenValueExtractor: TokenValueExtractor
    ) = TwoFactorInitSetupTokenService(authenticationService, hashService, jwtService, jwtProperties, tokenValueExtractor)

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorSetupTokenService(
        authenticationService: AuthenticationService,
        jwtService: JwtService,
        jwtProperties: JwtProperties,
        tokenValueExtractor: TokenValueExtractor
    ) = TwoFactorSetupTokenService(authenticationService, jwtService, jwtProperties)

    @Bean
    @ConditionalOnMissingBean
    fun userTwoFactorAuthService(
        userService: UserService,
        twoFactorAuthService: TwoFactorAuthService,
        authenticationService: AuthenticationService,
        twoFactorAuthProperties: TwoFactorAuthProperties,
        hashService: HashService,
        accessTokenCache: AccessTokenCache,
        userMapper: UserMapper,
        stepUpTokenService: StepUpTokenService,
        initTokenService: TwoFactorInitSetupTokenService,
        setupTokenService: TwoFactorSetupTokenService,
        loginTokenService: TwoFactorLoginTokenService
    ): UserTwoFactorAuthService {
        return UserTwoFactorAuthService(
            userService,
            twoFactorAuthService,
            authenticationService,
            twoFactorAuthProperties,
            hashService,
            accessTokenCache,
            userMapper,
            stepUpTokenService,
            initTokenService,
            setupTokenService,
            loginTokenService
        )
    }
}
