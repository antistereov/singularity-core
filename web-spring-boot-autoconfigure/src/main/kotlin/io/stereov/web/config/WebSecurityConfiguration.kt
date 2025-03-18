package io.stereov.web.config

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.filter.CookieAuthenticationFilter
import io.stereov.web.filter.LoggingFilter
import io.stereov.web.filter.RateLimitingFilter
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.properties.AuthProperties
import io.stereov.web.properties.FrontendProperties
import io.stereov.web.properties.RateLimitProperties
import io.stereov.web.user.service.UserService
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

@Configuration
@EnableWebFluxSecurity
@EnableMethodSecurity(prePostEnabled = true)
@AutoConfiguration(
    after = [
        MongoReactiveAutoConfiguration::class,
        SpringDataWebAutoConfiguration::class,
        RedisAutoConfiguration::class,
        ApplicationConfiguration::class,
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
    fun authenticationEntryPoint(): ServerAuthenticationEntryPoint {
        return HttpStatusServerEntryPoint(HttpStatus.UNAUTHORIZED)
    }

    @Bean
    @ConditionalOnMissingBean
    fun filterChain(
        http: ServerHttpSecurity,
        authProperties: AuthProperties,
        frontendProperties: FrontendProperties,
        jwtService: JwtService,
        userService: UserService,
        authenticationService: AuthenticationService,
        proxyManager: LettuceBasedProxyManager<String>,
        rateLimitProperties: RateLimitProperties,
    ): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource(frontendProperties)) }
            .httpBasic { it.disable() }
            .formLogin { it.disable() }
            .exceptionHandling {
                it.authenticationEntryPoint(authenticationEntryPoint())
            }
            .authorizeExchange {
                it.pathMatchers(
                    "/user/login",
                    "/user/refresh",
                    "user/register",
                    "user/2fa/verify",
                    "user/2fa/status",
                    "user/2fa/recovery"
                ).permitAll()
                authProperties.publicPaths.forEach { path ->
                    it.pathMatchers(path).permitAll()
                }
                authProperties.userPaths.forEach { path ->
                    it.pathMatchers(path).hasRole("USER")
                }
                authProperties.adminPaths.forEach { path ->
                    it.pathMatchers(path).hasRole("ADMIN")
                }
                it.anyExchange().authenticated()
            }
            .addFilterBefore(LoggingFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterBefore(RateLimitingFilter(authenticationService, proxyManager, rateLimitProperties), SecurityWebFiltersOrder.AUTHENTICATION)
            .addFilterBefore(CookieAuthenticationFilter(jwtService, userService), SecurityWebFiltersOrder.AUTHENTICATION)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .build()
    }

    @Bean
    @ConditionalOnMissingBean
    fun corsConfigurationSource(frontendProperties: FrontendProperties): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf(frontendProperties.baseUrl)
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("Authorization", "Content-Type")
        configuration.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
