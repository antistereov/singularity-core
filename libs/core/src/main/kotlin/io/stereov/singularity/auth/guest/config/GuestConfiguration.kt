package io.stereov.singularity.auth.guest.config

import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.token.component.CookieCreator
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthorizationService
import io.stereov.singularity.auth.core.service.EmailVerificationService
import io.stereov.singularity.auth.token.service.AccessTokenService
import io.stereov.singularity.auth.token.service.RefreshTokenService
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.guest.controller.GuestController
import io.stereov.singularity.auth.guest.exception.handler.GuestExceptionHandler
import io.stereov.singularity.auth.guest.mapper.GuestMapper
import io.stereov.singularity.auth.guest.service.GuestService
import io.stereov.singularity.auth.twofactor.properties.TwoFactorEmailCodeProperties
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.email.core.properties.EmailProperties
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.user.core.mapper.UserMapper
import io.stereov.singularity.user.core.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
    ]
)
class GuestConfiguration {

    // Controller
    
    @Bean
    @ConditionalOnMissingBean
    fun guestController(
        accessTokenService: AccessTokenService,
        refreshTokenService: RefreshTokenService,
        userMapper: UserMapper,
        guestService: GuestService,
        authProperties: AuthProperties,
        geolocationService: GeolocationService,
        cookieCreator: CookieCreator,
        authorizationService: AuthorizationService
    ) = GuestController(
        accessTokenService,
        refreshTokenService,
        userMapper,
        guestService,
        authProperties,
        geolocationService,
        cookieCreator,
        authorizationService
    )
    
    // ExceptionHandler

    @Bean
    @ConditionalOnMissingBean
    fun guestExceptionHandler() = GuestExceptionHandler()
    
    // Mapper
    
    @Bean
    @ConditionalOnMissingBean
    fun guestMapper() = GuestMapper()
    
    // Service
    
    @Bean
    @ConditionalOnMissingBean
    fun guestService(
        userService: UserService,
        twoFactorEmailCodeProperties: TwoFactorEmailCodeProperties,
        authorizationService: AuthorizationService,
        emailProperties: EmailProperties,
        hashService: HashService,
        emailVerificationService: EmailVerificationService,
        accessTokenCache: AccessTokenCache,
        guestMapper: GuestMapper,
    ) = GuestService(
        userService,
        twoFactorEmailCodeProperties,
        authorizationService,
        emailProperties,
        hashService,
        emailVerificationService,
        accessTokenCache,
        guestMapper,
    )
    
}