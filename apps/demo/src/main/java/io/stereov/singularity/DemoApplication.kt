package io.stereov.singularity

import io.swagger.v3.oas.annotations.OpenAPIDefinition
import io.swagger.v3.oas.annotations.info.Contact
import io.swagger.v3.oas.annotations.info.Info
import io.swagger.v3.oas.annotations.info.License
import io.swagger.v3.oas.annotations.servers.Server
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@OpenAPIDefinition(
    info = Info(
        title = "Singularity API",
        description = """
            Welcome to the Singularity! 
            
            This documentation covers the ready-to-use endpoints 
            for authentication, user management, and content management that come pre-configured with 
            Singularity.

            Features at a Glance:
            
            - Authentication & User Management: Includes JWT auth with refresh tokens, 2FA, 
              email verification, and role-based access control.
              
            - Data & Content Management: Endpoints for interacting with a content management 
              system that supports multi-language content, configurable tagging, and built-in 
              access control. A prebuilt Article class is also provided.
              
            - Performance & Security: The API is built with a focus on production-readiness, 
              including rate limiting and secret manager integration with automated key rotation.
            
            This API is a foundation for building robust backend services, microservices, or 
            full-stack applications.
            
            Note: Some endpoints require explicit configuration to be enabled.
            """,
        version = "1.0.0",
                contact = Contact(
                    name = "Stereov",
                    url = "https://stereov.io",
                    email = "contact@stereov.io"
                ),
    license = License(
        name = "GPLv3 License",
        url = "https://www.gnu.org/licenses/gpl-3.0.en.html"
    )
    ),
    servers = [
        Server(url = "https://singularity.stereov.io", description = "Demo server"),
        Server(url = "http://localhost:8000", description = "Local development server")
    ]
)
class DemoApplication

fun main() {
    runApplication<DemoApplication>()
}
