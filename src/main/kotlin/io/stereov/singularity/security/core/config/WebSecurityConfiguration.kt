package io.stereov.singularity.security.core.config

import io.stereov.singularity.auth.core.config.AuthenticationConfiguration
import io.stereov.singularity.auth.core.filter.AuthenticationFilter
import io.stereov.singularity.auth.core.properties.AuthProperties
import io.stereov.singularity.auth.geolocation.properties.GeolocationProperties
import io.stereov.singularity.auth.geolocation.service.GeolocationService
import io.stereov.singularity.auth.core.service.AccessTokenService
import io.stereov.singularity.global.filter.LoggingFilter
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.ratelimit.filter.RateLimitFilter
import io.stereov.singularity.ratelimit.service.RateLimitService
import io.stereov.singularity.security.core.properties.SecurityProperties
import io.stereov.singularity.user.core.model.Role
import io.stereov.singularity.user.core.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
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
 * It runs after the [org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration], [org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration],
 * [org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration], [io.stereov.singularity.global.config.ApplicationConfiguration] and [io.stereov.singularity.auth.core.config.AuthenticationConfiguration] classes to ensure that
 * the necessary configurations are applied in the correct order.
 *
 * It enables the following services:
 * - [AccessTokenService]
 * - [io.stereov.singularity.user.core.service.UserService]
 *
 * It enables the following beans:
 * - [org.springframework.security.crypto.password.PasswordEncoder]
 * - [org.springframework.security.web.server.ServerAuthenticationEntryPoint]
 * - [org.springframework.security.web.server.SecurityWebFilterChain]
 * - [org.springframework.web.cors.reactive.CorsConfigurationSource]
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
@EnableConfigurationProperties(SecurityProperties::class)
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
        rateLimitService: RateLimitService, geolocationProperties: GeolocationProperties,
        geoLocationService: GeolocationService,
        securityProperties: SecurityProperties
    ): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource(uiProperties, securityProperties)) }
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
            .addFilterBefore(RateLimitFilter(rateLimitService, geolocationProperties), SecurityWebFiltersOrder.FIRST)
            .addFilterBefore(LoggingFilter(geolocationProperties, geoLocationService), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterBefore(AuthenticationFilter(accessTokenService, userService), SecurityWebFiltersOrder.AUTHENTICATION)
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
    fun corsConfigurationSource(uiProperties: UiProperties, securityProperties: SecurityProperties): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        val allowedOrigins = mutableListOf<String>()
        allowedOrigins.addAll(securityProperties.allowedOrigins)
        allowedOrigins.add(uiProperties.baseUrl)

        configuration.allowedOrigins = allowedOrigins
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
