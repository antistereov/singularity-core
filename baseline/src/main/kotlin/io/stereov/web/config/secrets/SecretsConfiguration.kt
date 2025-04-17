package io.stereov.web.config.secrets

import io.stereov.web.config.ApplicationConfiguration
import io.stereov.web.global.service.secrets.service.KeyRotationService
import io.stereov.web.properties.secrets.KeyManagerProperties
import io.stereov.web.user.service.UserService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
    ]
)
@EnableConfigurationProperties(KeyManagerProperties::class)
class SecretsConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun keyRotationService(userService: UserService): KeyRotationService {
        return KeyRotationService(userService)
    }
}
