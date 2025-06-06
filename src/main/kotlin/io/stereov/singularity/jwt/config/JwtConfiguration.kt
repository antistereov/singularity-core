package io.stereov.singularity.jwt.config

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.OctetSequenceKey
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.SignedJWT
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.jwt.exception.handler.TokenExceptionHandler
import io.stereov.singularity.jwt.exception.model.InvalidTokenException
import io.stereov.singularity.jwt.properties.JwtProperties
import io.stereov.singularity.jwt.service.JwtSecretService
import io.stereov.singularity.jwt.service.JwtService
import io.stereov.singularity.secrets.component.KeyManager
import io.stereov.singularity.secrets.config.SecretsConfiguration
import kotlinx.coroutines.reactor.mono
import kotlinx.coroutines.runBlocking
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
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

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
        SecretsConfiguration::class,
    ]
)
@EnableConfigurationProperties(JwtProperties::class)
class JwtConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun jwtSecretService(keyManager: KeyManager, appProperties: AppProperties): JwtSecretService {
        return JwtSecretService(keyManager, appProperties)
    }

    @Bean
    @ConditionalOnMissingBean
    fun reactiveJwtDecoder(keyManager: KeyManager): ReactiveJwtDecoder {
        return ReactiveJwtDecoder { token ->
            mono {
                val jwt = try {
                    SignedJWT.parse(token)
                } catch (e: Exception) {
                    throw InvalidTokenException("Cannot parse token: $e", e)
                }
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
    fun jwtEncoder(jwtSecretService: JwtSecretService): JwtEncoder {
        val jwkSource = JWKSource<SecurityContext> { _, _ ->
            val secret = runBlocking { jwtSecretService.getCurrentSecret().value.toByteArray() }
            val secretKey = SecretKeySpec(secret, "HmacSHA256")
            val jwk = OctetSequenceKey.Builder(secretKey)
                .algorithm(JWSAlgorithm.HS256)
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
        jwtSecretService: JwtSecretService
    ): JwtService {
        return JwtService(jwtDecoder, jwtEncoder, jwtSecretService)
    }

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun tokenExceptionHandler(): TokenExceptionHandler {
        return TokenExceptionHandler()
    }
}
