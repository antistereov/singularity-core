package io.stereov.singularity.auth.token.config

import io.stereov.singularity.auth.core.cache.AccessTokenCache
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.geolocation.properties.GeolocationProperties
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.token.component.CookieCreator
import io.stereov.singularity.auth.token.component.TokenValueExtractor
import io.stereov.singularity.auth.token.service.*
import io.stereov.singularity.database.encryption.service.EncryptionService
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
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
class TokenConfiguration {

    // Component

    @Bean
    @ConditionalOnMissingBean
    fun cookieCreator(appProperties: AppProperties) = CookieCreator(appProperties)

    @Bean
    @ConditionalOnMissingBean
    fun tokenValueExtractor(authProperties: AuthProperties) = TokenValueExtractor(authProperties)

    // Services

    @Bean
    @ConditionalOnMissingBean
    fun accessTokenService(
        jwtService: JwtService,
        accessTokenCache: AccessTokenCache,
        jwtProperties: JwtProperties,
        tokenValueExtractor: TokenValueExtractor,
    ) = AccessTokenService(
        jwtService,
        accessTokenCache,
        jwtProperties,
        tokenValueExtractor,
    )

    @Bean
    @ConditionalOnMissingBean
    fun emailVerificationTokenService(
        jwtProperties: JwtProperties,
        jwtService: JwtService
    ) = EmailVerificationTokenService(
        jwtProperties,
        jwtService
    )

    @Bean
    @ConditionalOnMissingBean
    fun passwordResetTokenService(
        jwtProperties: JwtProperties,
        jwtService: JwtService,
        encryptionService: EncryptionService,
    ) = PasswordResetTokenService(
        jwtProperties, jwtService, encryptionService
    )

    @Bean
    @ConditionalOnMissingBean
    fun refreshTokenService(
        jwtService: JwtService,
        jwtProperties: JwtProperties,
        geolocationService: GeolocationService,
        geolocationProperties: GeolocationProperties,
        userService: UserService,
        tokenValueExtractor: TokenValueExtractor,
    ) = RefreshTokenService(
        jwtService,
        jwtProperties,
        geolocationService,
        geolocationProperties,
        userService,
        tokenValueExtractor,
    )

    @Bean
    @ConditionalOnMissingBean
    fun sessionTokenService(
        jwtService: JwtService,
        jwtProperties: JwtProperties,
        tokenValueExtractor: TokenValueExtractor
    ) = SessionTokenService(
        jwtService,
        jwtProperties,
        tokenValueExtractor
    )
    
    @Bean
    @ConditionalOnMissingBean
    fun stepUpTokenService(
        jwtService: JwtService,
        jwtProperties: JwtProperties,
        tokenValueExtractor: TokenValueExtractor,
    ): StepUpTokenService {
        return StepUpTokenService(
            jwtService,
            jwtProperties,
            tokenValueExtractor
        )
    }

}
