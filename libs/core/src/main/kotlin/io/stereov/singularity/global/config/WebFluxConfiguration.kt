package io.stereov.singularity.global.config

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.format.FormatterRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer

@AutoConfiguration
class WebFluxConfiguration : WebFluxConfigurer {

    override fun addFormatters(registry: FormatterRegistry) {
    }
}
