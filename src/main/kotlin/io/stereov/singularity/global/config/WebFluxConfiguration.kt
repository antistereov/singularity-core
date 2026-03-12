package io.stereov.singularity.global.config

import io.stereov.singularity.database.core.model.DocumentKey
import io.stereov.singularity.file.core.model.FileRenditionKey
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.core.convert.converter.Converter
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

    class StringToDocumentKeyConverter : Converter<String, DocumentKey> {
        override fun convert(source: String): DocumentKey =
            DocumentKey(source)
    }

    class StringToFileRenditionKeyConverter : Converter<String, FileRenditionKey> {
        override fun convert(source: String): FileRenditionKey =
            FileRenditionKey(source)
    }

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(StringToDocumentKeyConverter())
        registry.addConverter(StringToFileRenditionKeyConverter())
    }
}
