package io.stereov.singularity.auth.config

import io.stereov.singularity.auth.exception.handler.AuthExceptionHandler
import io.stereov.singularity.auth.properties.AuthProperties
import io.stereov.singularity.auth.service.AuthenticationService
import io.stereov.singularity.auth.service.CookieService
import io.stereov.singularity.file.s3.config.S3Configuration
import io.stereov.singularity.geolocation.service.GeoLocationService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.jwt.properties.JwtProperties
import io.stereov.singularity.twofactorauth.service.TwoFactorAuthService
import io.stereov.singularity.user.service.UserService
import io.stereov.singularity.user.service.token.TwoFactorAuthTokenService
import io.stereov.singularity.user.service.token.UserTokenService
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

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun authExceptionHandler(): AuthExceptionHandler {
        return AuthExceptionHandler()
    }
}
