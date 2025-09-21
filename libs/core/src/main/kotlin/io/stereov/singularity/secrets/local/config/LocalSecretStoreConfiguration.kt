package io.stereov.singularity.secrets.local.config

import io.r2dbc.h2.H2ConnectionConfiguration
import io.r2dbc.h2.H2ConnectionFactory
import io.r2dbc.spi.ConnectionFactory
import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.secrets.core.component.SecretCache
import io.stereov.singularity.secrets.core.component.SecretStore
import io.stereov.singularity.secrets.local.component.LocalSecretStore
import io.stereov.singularity.secrets.local.properties.LocalSecretStoreProperties
import io.stereov.singularity.secrets.local.repository.LocalSecretRepository
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate
import org.springframework.data.r2dbc.dialect.H2Dialect
import org.springframework.r2dbc.core.DatabaseClient
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.Path
import kotlin.io.path.absolute

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
    ]
)
@EnableConfigurationProperties(LocalSecretStoreProperties::class)
@ConditionalOnProperty(prefix = "singularity.secrets", value = ["store"], havingValue = "local", matchIfMissing = true)
class LocalSecretStoreConfiguration(
    private val properties: LocalSecretStoreProperties
) {

    @PostConstruct
    fun initialize() {
        Files.createDirectories(Paths.get(properties.secretDirectory))
    }

    @Bean
    fun connectionFactory(): ConnectionFactory {
        val filePath = Path(properties.secretDirectory).resolve("secrets").normalize()

        return H2ConnectionFactory(
            H2ConnectionConfiguration.builder()
                .file(filePath.absolute().toString())
                .username("sa")
                .build()
        )
    }

    @Bean
    fun secretsEntityTemplate(connectionFactory: ConnectionFactory): R2dbcEntityTemplate {
        val client = DatabaseClient.create(connectionFactory)
        return R2dbcEntityTemplate(client, H2Dialect.INSTANCE)
    }

    @Bean
    @ConditionalOnMissingBean
    fun localSecretRepository(template: R2dbcEntityTemplate): LocalSecretRepository {
        return LocalSecretRepository(template)
    }

    @Bean
    @ConditionalOnMissingBean
    fun localSecretStore(
        repository: LocalSecretRepository,
        secretCache: SecretCache
    ): SecretStore {
        return LocalSecretStore(repository, secretCache)
    }

}
