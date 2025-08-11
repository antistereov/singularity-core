package io.stereov.singularity.content.translate.config

import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.content.translate.service.TranslateService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration(after = [
    ApplicationConfiguration::class
])
class TranslateConfiguration {


    @Bean
    @ConditionalOnMissingBean
    fun translateService() = TranslateService()
}
