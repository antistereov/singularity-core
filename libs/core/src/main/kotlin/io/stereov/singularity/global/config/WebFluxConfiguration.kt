package io.stereov.singularity.global.config

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.data.web.ReactivePageableHandlerMethodArgumentResolver
import org.springframework.format.FormatterRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer

@AutoConfiguration
class WebFluxConfiguration : WebFluxConfigurer {

    override fun configureArgumentResolvers(configurer: ArgumentResolverConfigurer) {
        super.configureArgumentResolvers(configurer)
        configurer.addCustomResolver(ReactivePageableHandlerMethodArgumentResolver())
    }

    override fun addFormatters(registry: FormatterRegistry) {
    }
}
