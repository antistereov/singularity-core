package io.stereov.singularity.auth.twofactor.config

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.CookieCreator
import io.stereov.singularity.auth.core.service.TokenValueExtractor
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.service.AccessTokenService
import io.stereov.singularity.auth.core.service.RefreshTokenService
import io.stereov.singularity.auth.twofactor.controller.TwoFactorAuthenticationController
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
        twoFactorAuthenticationService: TwoFactorAuthenticationService,
        authProperties: AuthProperties,
        geolocationService: GeolocationService,
        userMapper: UserMapper,
        accessTokenService: AccessTokenService,
        refreshTokenService: RefreshTokenService,
        setupInitTokenService: TwoFactorInitSetupTokenService,
        stepUpTokenService: StepUpTokenService,
        cookieCreator: CookieCreator,
        authorizationService: AuthorizationService
    ): TwoFactorAuthenticationController {
        return TwoFactorAuthenticationController(
            twoFactorAuthenticationService,
            authProperties,
            geolocationService,
            userMapper,
            accessTokenService,
            refreshTokenService,
            setupInitTokenService,
            stepUpTokenService,
            cookieCreator,
            authorizationService
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
        authorizationService: AuthorizationService,
        twoFactorService: TwoFactorService,
        tokenValueExtractor: TokenValueExtractor,
    ): StepUpTokenService {
        return StepUpTokenService(jwtService, jwtProperties, authorizationService, twoFactorService, tokenValueExtractor)
    }

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorAuthService(googleAuthenticator: GoogleAuthenticator): TwoFactorService {
        return TwoFactorService(googleAuthenticator)
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
        authorizationService: AuthorizationService,
        hashService: HashService,
        jwtService: JwtService,
        jwtProperties: JwtProperties,
        tokenValueExtractor: TokenValueExtractor
    ) = TwoFactorInitSetupTokenService(authorizationService, hashService, jwtService, jwtProperties, tokenValueExtractor)

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorSetupTokenService(
        authorizationService: AuthorizationService,
        jwtService: JwtService,
        jwtProperties: JwtProperties,
    ) = TwoFactorSetupTokenService(authorizationService, jwtService, jwtProperties)

    @Bean
    @ConditionalOnMissingBean
    fun userTwoFactorAuthService(
        userService: UserService,
        twoFactorService: TwoFactorService,
        authorizationService: AuthorizationService,
        twoFactorAuthProperties: TwoFactorAuthProperties,
        hashService: HashService,
        accessTokenCache: AccessTokenCache,
        userMapper: UserMapper,
        stepUpTokenService: StepUpTokenService,
        initTokenService: TwoFactorInitSetupTokenService,
        setupTokenService: TwoFactorSetupTokenService,
        loginTokenService: TwoFactorLoginTokenService
    ): TwoFactorAuthenticationService {
        return TwoFactorAuthenticationService(
            userService,
            twoFactorService,
            authorizationService,
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
