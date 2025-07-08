package io.stereov.singularity.file.local.config

import io.stereov.singularity.file.local.properties.LocalFileStorageProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.config.ResourceHandlerRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer

@Configuration
@ConditionalOnProperty(prefix = "singularity.file.storage", value = ["type"], havingValue = "local", matchIfMissing = true)
class FileSystemResourceConfiguration(
    private val properties: LocalFileStorageProperties
) : WebFluxConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/api/assets/public/**")
            .addResourceLocations("file:${properties.publicPath}")
    }
}