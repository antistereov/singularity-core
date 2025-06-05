package io.stereov.singularity.config

import io.stereov.singularity.global.language.util.LanguageToStringConverter
import io.stereov.singularity.global.language.util.StringToLanguageConverter
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.format.FormatterRegistry
import org.springframework.web.reactive.config.WebFluxConfigurer

@AutoConfiguration
class WebFluxConfiguration : WebFluxConfigurer {

    override fun addFormatters(registry: FormatterRegistry) {
        registry.addConverter(StringToLanguageConverter())
        registry.addConverter(LanguageToStringConverter())
    }
}
