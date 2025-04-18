package io.stereov.web.config

import com.nimbusds.jose.Algorithm
import com.nimbusds.jose.jwk.OctetSequenceKey
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.SignedJWT
import io.stereov.web.config.secrets.SecretsConfiguration
import io.stereov.web.global.service.jwt.JwtService
import io.stereov.web.global.service.jwt.exception.model.InvalidTokenException
import io.stereov.web.global.service.secrets.component.KeyManager
import kotlinx.coroutines.reactor.mono
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import java.time.Instant
import java.util.*
import javax.crypto.Mac
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
        ApplicationConfiguration::class,
        SecretsConfiguration::class,
    ]
)
class JwtConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun reactiveJwtDecoder(keyManager: KeyManager): ReactiveJwtDecoder {
        return ReactiveJwtDecoder { token ->
            mono {
                val jwt = SignedJWT.parse(token)
                val keyId = jwt.header.keyID

                if (keyId.isNullOrEmpty()) throw InvalidTokenException("No key for JWT secret found in header")
                val secret = keyManager.getSecretById(UUID.fromString(keyId))

                if (!verifyJwtSignature(jwt, secret.value)) throw InvalidTokenException("Signature is invalid.")

                val claims = jwt.jwtClaimsSet.claims.mapValues { (key, value) ->
                    when (key) {
                        "iat", "exp", "nbf" -> when (value) {
                            is Number -> Instant.ofEpochSecond(value.toLong())
                            is Instant -> value
                            is String -> Instant.parse(value)
                            is Date -> value.toInstant()
                            else -> throw InvalidTokenException("Date claims require Instant values: ${value.javaClass.name}")
                        }
                        else -> value
                    }
                }

                Jwt.withTokenValue(token)
                    .header("kid", keyId)
                    .claims { it.putAll(claims) }
                    .build()
            }
        }
    }

    private fun verifyJwtSignature(jwt: SignedJWT, secret: String): Boolean {
        val keySpec = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(keySpec)

        val signedPart = jwt.parsedString.substringBeforeLast(".")
        val expectedSignature = mac.doFinal(signedPart.toByteArray())

        val actualSignature = jwt.signature.decode()
        return expectedSignature.contentEquals(actualSignature)
    }

    @Bean
    @ConditionalOnMissingBean
    fun jwtEncoder(keyManager: KeyManager): JwtEncoder {
        val jwkSource = JWKSource<SecurityContext> { _, _ ->
            val secret = keyManager.getJwtSecret().value.toByteArray()
            val secretKey = SecretKeySpec(secret, "HmacSHA256")
            val jwk = OctetSequenceKey.Builder(secretKey)
                .algorithm(Algorithm.parse("HmacSHA256"))
                .build()
            listOf(jwk)
        }
        return NimbusJwtEncoder(jwkSource)
    }

    @Bean
    @ConditionalOnMissingBean
    fun jwtService(
        jwtDecoder: ReactiveJwtDecoder,
        jwtEncoder: JwtEncoder,
        keyManager: KeyManager
    ): JwtService {
        return JwtService(jwtDecoder, jwtEncoder, keyManager)
    }
}
