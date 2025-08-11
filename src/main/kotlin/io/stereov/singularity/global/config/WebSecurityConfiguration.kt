package io.stereov.singularity.global.config

import io.stereov.singularity.auth.core.config.AuthenticationConfiguration
import io.stereov.singularity.auth.core.filter.CookieAuthenticationFilter
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.global.filter.LoggingFilter
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.ratelimit.filter.RateLimitFilter
import io.stereov.singularity.ratelimit.service.RateLimitService
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.service.UserService
import io.stereov.singularity.user.token.service.AccessTokenService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

/**
 * # Configuration class for web security.
 *
 * This class is responsible for configuring the web security settings
 * and components in the application.
 *
 * It runs after the [MongoReactiveAutoConfiguration], [SpringDataWebAutoConfiguration],
 * [RedisAutoConfiguration], [ApplicationConfiguration] and [AuthenticationConfiguration] classes to ensure that
 * the necessary configurations are applied in the correct order.
 *
 * It enables the following services:
 * - [AccessTokenService]
 * - [UserService]
 *
 * It enables the following beans:
 * - [PasswordEncoder]
 * - [ServerAuthenticationEntryPoint]
 * - [SecurityWebFilterChain]
 * - [CorsConfigurationSource]
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Configuration
@EnableWebFluxSecurity
@EnableMethodSecurity(prePostEnabled = true)
@AutoConfiguration(
    after = [
        AuthenticationConfiguration::class
    ]
)
class WebSecurityConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    @ConditionalOnMissingBean
    fun filterChain(
        http: ServerHttpSecurity,
        authProperties: AuthProperties,
        uiProperties: UiProperties,
        accessTokenService: AccessTokenService,
        userService: UserService,
        rateLimitService: RateLimitService,
    ): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource(uiProperties)) }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint())
            }
            .authorizeExchange {
                authProperties.publicPaths.forEach { path ->
                    it.pathMatchers(path).permitAll()
                }
                authProperties.userPaths.forEach { path ->
                    it.pathMatchers(path).hasRole("USER")
                }
                authProperties.adminPaths.forEach { path ->
                    it.pathMatchers(path).hasRole("ADMIN")
                }
                it.pathMatchers("/admin/**").hasRole(Role.ADMIN.name)
                it.anyExchange().permitAll()
            }
            .addFilterBefore(RateLimitFilter(rateLimitService), SecurityWebFiltersOrder.FIRST)
            .addFilterBefore(LoggingFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterBefore(CookieAuthenticationFilter(accessTokenService, userService, authProperties), SecurityWebFiltersOrder.AUTHENTICATION)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .build()
    }

    @Bean
    @ConditionalOnMissingBean
    fun authenticationEntryPoint(): ServerAuthenticationEntryPoint {
        return HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)
    }

    @Bean
    @ConditionalOnMissingBean
    fun corsConfigurationSource(uiProperties: UiProperties): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf(uiProperties.baseUrl)
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
