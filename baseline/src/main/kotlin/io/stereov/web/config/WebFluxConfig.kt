package io.stereov.web.config

import io.stereov.web.properties.FileProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Configuration
import org.springframework.http.CacheControl
import org.springframework.web.reactive.config.ResourceHandlerRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import java.nio.file.Path
import java.util.concurrent.TimeUnit

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
    ]
)
class WebFluxConfig(
    private val fileProperties: FileProperties
) : WebFluxConfigurer {

    init {
        println("📂 Absolute path: ${Path.of(fileProperties.basePath.removePrefix("file:")).toAbsolutePath()}")
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/static/**")
            .addResourceLocations("file:${fileProperties.basePath}")
            .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS))
    }
}
