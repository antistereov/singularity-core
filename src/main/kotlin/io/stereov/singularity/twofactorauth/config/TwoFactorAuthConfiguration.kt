package io.stereov.singularity.twofactorauth.config

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.stereov.singularity.auth.service.AuthenticationService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.hash.service.HashService
import io.stereov.singularity.jwt.properties.JwtProperties
import io.stereov.singularity.jwt.service.JwtService
import io.stereov.singularity.twofactorauth.exception.handler.TwoFactorAuthExceptionHandler
import io.stereov.singularity.twofactorauth.properties.TwoFactorAuthProperties
import io.stereov.singularity.twofactorauth.service.TwoFactorAuthService
import io.stereov.singularity.user.service.token.TwoFactorAuthTokenService
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

    @Bean
    @ConditionalOnMissingBean
    fun googleAuthenticator(): GoogleAuthenticator {
        return GoogleAuthenticator()
    }

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorAuthService(googleAuthenticator: GoogleAuthenticator): TwoFactorAuthService {
        return TwoFactorAuthService(googleAuthenticator)
    }

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorAuthTokenService(jwtService: JwtService, jwtProperties: JwtProperties, authenticationService: AuthenticationService, twoFactorAuthService: TwoFactorAuthService, hashService: HashService): TwoFactorAuthTokenService {
        return TwoFactorAuthTokenService(jwtService, jwtProperties, authenticationService, twoFactorAuthService, hashService)
    }

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorAuthExceptionHandler() = TwoFactorAuthExceptionHandler()
}
