package io.stereov.singularity.global.config

import io.stereov.singularity.auth.config.AuthenticationConfiguration
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.tags.Tag
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        AuthenticationConfiguration::class
    ]
)
class OpenApiConfiguration {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info().title("Singularity Core API").version("1.0.0")
            )
            .addTagsItem(
                Tag().name("User Session").description("Login, logout and session endpoints")
            )
    }
}