package io.stereov.singularity.auth.token.config

import io.lettuce.core.ExperimentalLettuceCoroutinesApi
import io.lettuce.core.api.coroutines.RedisCoroutinesCommands
import io.stereov.singularity.auth.core.service.AuthenticationService
import io.stereov.singularity.database.hash.service.HashService
import io.stereov.singularity.auth.jwt.properties.JwtProperties
import io.stereov.singularity.auth.jwt.service.JwtService
import io.stereov.singularity.auth.twofactor.service.TwoFactorAuthService
import io.stereov.singularity.user.core.config.UserConfiguration
import io.stereov.singularity.auth.token.cache.AccessTokenCache
import io.stereov.singularity.auth.token.service.AccessTokenService
import io.stereov.singularity.auth.token.service.TwoFactorTokenService
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
class UserTokenConfiguration {

    // Cache

    @Bean
    @ConditionalOnMissingBean
    @OptIn(ExperimentalLettuceCoroutinesApi::class)
    fun accessTokenCache(
        commands: RedisCoroutinesCommands<String, ByteArray>,
        jwtProperties: JwtProperties,
    ): AccessTokenCache {
        return AccessTokenCache(commands, jwtProperties)
    }

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun twoFactorTokenService(
        jwtService: JwtService,
        jwtProperties: JwtProperties,
        authenticationService: AuthenticationService,
        twoFactorAuthService: TwoFactorAuthService,
        hashService: HashService
    ) = TwoFactorTokenService(jwtService, jwtProperties, authenticationService, twoFactorAuthService, hashService)

    @Bean
    @ConditionalOnMissingBean
    fun accessTokenService(
        jwtService: JwtService,
        accessTokenCache: AccessTokenCache,
        jwtProperties: JwtProperties,
    ): AccessTokenService {
        return AccessTokenService(jwtService, accessTokenCache, jwtProperties)
    }
}
