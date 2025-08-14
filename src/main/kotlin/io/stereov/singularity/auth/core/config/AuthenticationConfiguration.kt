package io.stereov.singularity.auth.core.config

import io.stereov.singularity.auth.core.exception.handler.AuthExceptionHandler
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.auth.core.service.CookieService
import io.stereov.singularity.auth.geolocation.properties.GeolocationProperties
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.twofactor.service.TwoFactorAuthService
import io.stereov.singularity.file.s3.config.S3Configuration
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.user.core.service.UserService
import io.stereov.singularity.user.token.service.AccessTokenService
import io.stereov.singularity.user.token.service.TwoFactorTokenService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
        S3Configuration::class,
    ]
)
@EnableConfigurationProperties(AuthProperties::class)
class AuthenticationConfiguration {

    // Services

    @Bean
    @ConditionalOnMissingBean
    fun authenticationService(): AuthenticationService {
        return AuthenticationService()
    }

    @Bean
    @ConditionalOnMissingBean
    fun cookieService(
        accessTokenService: AccessTokenService,
        jwtProperties: JwtProperties,
        appProperties: AppProperties,
        geoLocationService: GeolocationService,
        userService: UserService,
        twoFactorTokenService: TwoFactorTokenService,
        authenticationService: AuthenticationService,
        twoFactorAuthService: TwoFactorAuthService,
        geolocationProperties: GeolocationProperties
    ): CookieService {
        return CookieService(
            accessTokenService,
            jwtProperties,
            appProperties,
            geoLocationService,
            userService,
            twoFactorTokenService,
            authenticationService,
            twoFactorAuthService,
            geolocationProperties
        )
    }

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun authExceptionHandler(): AuthExceptionHandler {
        return AuthExceptionHandler()
    }
}
