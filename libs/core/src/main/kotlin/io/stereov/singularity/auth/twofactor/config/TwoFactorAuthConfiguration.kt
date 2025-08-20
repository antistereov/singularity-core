package io.stereov.singularity.auth.twofactor.config

import com.warrenstrange.googleauth.GoogleAuthenticator
import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.twofactor.exception.handler.TwoFactorAuthExceptionHandler
import io.stereov.singularity.auth.twofactor.properties.TwoFactorAuthProperties
import io.stereov.singularity.auth.twofactor.service.TwoFactorAuthService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.auth.token.service.TwoFactorTokenService
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
    fun twoFactorAuthTokenService(jwtService: JwtService, jwtProperties: JwtProperties, authenticationService: AuthenticationService, twoFactorAuthService: TwoFactorAuthService, hashService: HashService): TwoFactorTokenService {
        return TwoFactorTokenService(jwtService, jwtProperties, authenticationService, twoFactorAuthService, hashService)
    }

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorAuthExceptionHandler() = TwoFactorAuthExceptionHandler()
}
