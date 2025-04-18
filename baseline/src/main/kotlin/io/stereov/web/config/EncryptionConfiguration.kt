package io.stereov.web.config

import io.stereov.web.config.secrets.BitwardenSecretsConfiguration
import io.stereov.web.config.secrets.SecretsConfiguration
import io.stereov.web.global.service.encryption.service.EncryptionService
import io.stereov.web.global.service.secrets.component.KeyManager
import kotlinx.serialization.json.Json
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * # Configuration class for encryption-related beans.
 *
 * This class is responsible for configuring the authentication-related services
 * and components in the application.
 *
 * It runs after the [MongoReactiveAutoConfiguration], [SpringDataWebAutoConfiguration],
 * [RedisAutoConfiguration], and [ApplicationConfiguration] classes to ensure that
 * the necessary configurations are applied in the correct order.
 *
 * This class enables the following services:
 * - [EncryptionService]
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
@Configuration
@AutoConfiguration(
    after = [
        SecretsConfiguration::class,
        BitwardenSecretsConfiguration::class,
    ]
)
class EncryptionConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun encryptionTransformer(keyManager: KeyManager, json: Json): EncryptionService {
        return EncryptionService(keyManager, json)
    }
}
