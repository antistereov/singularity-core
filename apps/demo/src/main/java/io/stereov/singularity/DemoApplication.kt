package io.stereov.singularity

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.servers.Server
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@OpenAPIDefinition(
    info = Info(title = "Singularity Core API", version = "1.0.0"),
    servers = [
        Server(url = "https://singularity.stereov.io", description = "Demo server"),
        Server(url = "http://localhost:8000", description = "Local development server")
    ]
)
class DemoApplication

fun main() {
    runApplication<DemoApplication>()
}
