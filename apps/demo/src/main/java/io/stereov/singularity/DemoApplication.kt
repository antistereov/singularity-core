package io.stereov.singularity

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class DemoApplication {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info().title("Singularity Core API").version("1.0.0")
            )
            .addServersItem(
                Server().url("https://singularity.stereov.io").description("Demo server")
            )
            .addServersItem(
                Server().url("http://localhost:8000").description("Local development server")
            )
            .components(
                Components().addSecuritySchemes(
                    "bearerAuth",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            )
            .security(listOf<SecurityRequirement>(SecurityRequirement().addList("bearerAuth")))
    }
}



fun main() {
    runApplication<DemoApplication>()
}
