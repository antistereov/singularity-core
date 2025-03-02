package io.stereov.web.config

import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.global.service.geolocation.GeoLocationService
import io.stereov.web.global.service.hash.HashService
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.global.service.mail.MailService
import io.stereov.web.global.service.mail.MailVerificationCooldownService
import io.stereov.web.properties.*
import io.stereov.web.user.controller.UserSessionController
import io.stereov.web.user.repository.UserRepository
import io.stereov.web.user.service.CookieService
import io.stereov.web.user.service.UserService
import io.stereov.web.user.service.UserSessionService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@AutoConfiguration(
    after = [
        MongoReactiveAutoConfiguration::class,
        SpringDataWebAutoConfiguration::class,
        RedisAutoConfiguration::class,
        ApplicationConfiguration::class,
    ]
)
@EnableReactiveMongoRepositories(
    basePackageClasses = [UserRepository::class]
)
class AuthenticationConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun authenticationService(): AuthenticationService {
        return AuthenticationService()
    }

    @Bean
    @ConditionalOnMissingBean
    fun geoLocationService(webClient: WebClient): GeoLocationService {
        return GeoLocationService(webClient)
    }

    @Bean
    @ConditionalOnMissingBean
    fun hashService(): HashService {
        return HashService()
    }

    @Bean
    @ConditionalOnMissingBean
    fun userService(userRepository: UserRepository): UserService {
        return UserService(userRepository)
    }

    @Bean
    @ConditionalOnMissingBean
    fun userSessionController(
        authenticationService: AuthenticationService,
        userService: UserService,
        userSessionService: UserSessionService,
        cookieService: CookieService,
    ): UserSessionController {
        return UserSessionController(authenticationService, userService, userSessionService, cookieService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun userSessionService(
        userService: UserService,
        hashService: HashService,
        jwtService: JwtService,
        authenticationService: AuthenticationService,
        mailService: MailService,
        mailProperties: MailProperties,
        mailVerificationCooldownService: MailVerificationCooldownService,
    ): UserSessionService {
        return UserSessionService(
            userService,
            hashService,
            jwtService,
            authenticationService,
            mailService,
            mailProperties,
            mailVerificationCooldownService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun cookieService(
        jwtService: JwtService,
        jwtProperties: JwtProperties,
        backendProperties: BackendProperties,
        geoLocationService: GeoLocationService,
        userService: UserService,
    ): CookieService {
        return CookieService(jwtService, jwtProperties, backendProperties, geoLocationService, userService)
    }
}
