package io.stereov.web.config

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.properties.JwtProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import javax.crypto.spec.SecretKeySpec

/**
 * # Configuration class for JWT-related beans.
 *
 * This class is responsible for configuring the JWT-related services
 * and components in the application.
 *
 * It runs after the [MongoReactiveAutoConfiguration], [SpringDataWebAutoConfiguration],
 * [RedisAutoConfiguration], and [ApplicationConfiguration] classes to ensure that
 * the necessary configurations are applied in the correct order.
 *
 * This class enables the following services:
 * - [JwtService]
 *
 * It enables the following beans:
 * - [JwtEncoder]
 * - [ReactiveJwtDecoder]
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Configuration
@AutoConfiguration(
    after = [
        MongoReactiveAutoConfiguration::class,
        SpringDataWebAutoConfiguration::class,
        RedisAutoConfiguration::class,
        ApplicationConfiguration::class,
    ]
)
class JwtEncodingConfig {

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
    fun jwtService(
        jwtDecoder: ReactiveJwtDecoder,
        jwtEncoder: JwtEncoder,
    ): JwtService {
        return JwtService(jwtDecoder, jwtEncoder)
    }
}
