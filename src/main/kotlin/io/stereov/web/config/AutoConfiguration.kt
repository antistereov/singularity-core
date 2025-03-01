package io.stereov.web.config

import com.mongodb.reactivestreams.client.MongoClient
import com.mongodb.reactivestreams.client.MongoClients
import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy
import io.github.bucket4j.redis.lettuce.Bucket4jLettuce
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import io.stereov.web.auth.service.AuthenticationService
import io.stereov.web.filter.CookieAuthenticationFilter
import io.stereov.web.global.service.encryption.EncryptionService
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
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.SimpleReactiveMongoDatabaseFactory
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.ServerAuthenticationEntryPoint
import org.springframework.security.web.server.authentication.HttpStatusServerEntryPoint
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.reactive.function.client.WebClient
import java.time.Duration
import javax.crypto.spec.SecretKeySpec

@Configuration
@AutoConfiguration
@EnableConfigurationProperties(
    AuthProperties::class,
    BackendProperties::class,
    JwtProperties::class,
    EncryptionProperties::class,
    FrontendProperties::class,
    MailProperties::class,
    RateLimitProperties::class
)
@EnableReactiveMongoRepositories(
    basePackageClasses = [UserRepository::class]
)
@EnableWebFluxSecurity
@EnableMethodSecurity(prePostEnabled = true)
class AutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun authenticationService(): AuthenticationService {
        return AuthenticationService()
    }

    @Bean
    @ConditionalOnMissingBean
    fun jwtService(
        jwtDecoder: ReactiveJwtDecoder,
        jwtEncoder: JwtEncoder,
        jwtProperties: JwtProperties,
        mailProperties: MailProperties
    ): JwtService {
        return JwtService(jwtDecoder, jwtEncoder, jwtProperties, mailProperties)
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
    fun encryptionService(encryptionProperties: EncryptionProperties): EncryptionService {
        return EncryptionService(encryptionProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun mailService(
        mailSender: JavaMailSender,
        mailProperties: MailProperties,
        frontendProperties: FrontendProperties,
        jwtService: JwtService,
        mailVerificationCooldownService: MailVerificationCooldownService
    ): MailService {
        return MailService(mailSender, mailProperties, frontendProperties, jwtService, mailVerificationCooldownService)
    }

    @Bean
    @ConditionalOnMissingBean
    fun mailVerificationCooldownService(
        redisTemplate: ReactiveRedisTemplate<String, String>,
        mailProperties: MailProperties
    ): MailVerificationCooldownService {
        return MailVerificationCooldownService(redisTemplate, mailProperties)
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

    @Bean
    @ConditionalOnMissingBean
    fun webClient(): WebClient {
        return WebClient.builder()
            .build()
    }

    @Bean
    @ConditionalOnMissingBean
    fun reactiveJwtDecoder(jwtProperties: JwtProperties): ReactiveJwtDecoder {
        val jwtKey = jwtProperties.secretKey
        val secretKey = SecretKeySpec(jwtKey.toByteArray(), "HmacSHA256")

        return NimbusReactiveJwtDecoder.withSecretKey(secretKey).build()
    }

    @Bean
    @ConditionalOnMissingBean
    fun jwtEncoder(jwtProperties: JwtProperties): JwtEncoder {
        val jwtKey = jwtProperties.secretKey
        val secretKey = SecretKeySpec(jwtKey.toByteArray(), "HmacSHA256")
        val secret = ImmutableSecret<SecurityContext>(secretKey)
        return NimbusJwtEncoder(secret)
    }

    @Bean
    @ConditionalOnMissingBean
    fun statefulRedisConnection(redisProperties: RedisProperties): StatefulRedisConnection<String, ByteArray> {
        val redisUri = RedisURI.builder()
            .withHost(redisProperties.host)
            .withPort(redisProperties.port)
            .withSsl(redisProperties.ssl.isEnabled)
            .apply {
                if (!redisProperties.password.isNullOrEmpty()) {
                    withPassword(redisProperties.password?.toCharArray())
                }
                withDatabase(redisProperties.database)
            }
            .build()

        val redisClient = RedisClient.create(redisUri)
        return redisClient.connect(RedisCodec.of(StringCodec.UTF8, ByteArrayCodec()))
    }

    @Bean
    @ConditionalOnMissingBean
    fun bucketProxyManager(connection: StatefulRedisConnection<String, ByteArray>): LettuceBasedProxyManager<String> {
        return Bucket4jLettuce.casBasedBuilder(connection)
            .expirationAfterWrite(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofSeconds(10)))
            .build()
    }

    @Bean
    @ConditionalOnMissingBean
    fun javaMailSender(mailProperties: MailProperties): JavaMailSender {
        val mailSender = JavaMailSenderImpl()
        mailSender.host = mailProperties.host
        mailSender.port = mailProperties.port
        mailSender.username = mailProperties.username
        mailSender.password = mailProperties.password

        val props = mailSender.javaMailProperties
        props["mail.transport.protocol"] = mailProperties.transportProtocol
        props["mail.smtp.auth"] = mailProperties.smtpAuth
        props["mail.smtp.starttls.enable"] = mailProperties.smtpStarttls
        props["mail.debug"] = mailProperties.debug
        return mailSender
    }

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

    @Bean
    @ConditionalOnMissingBean
    fun reactiveMongoClient(properties: MongoProperties): MongoClient {
        return MongoClients.create(properties.uri)
    }

    @Bean
    @ConditionalOnMissingBean
    fun reactiveMongoTemplate(mongoClient: MongoClient, properties: MongoProperties): ReactiveMongoTemplate {
        return ReactiveMongoTemplate(SimpleReactiveMongoDatabaseFactory(mongoClient, properties.database))
    }
}
