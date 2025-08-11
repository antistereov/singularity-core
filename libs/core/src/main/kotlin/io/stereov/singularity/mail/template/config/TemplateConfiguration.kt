package io.stereov.singularity.mail.template.config

import io.stereov.singularity.global.config.ApplicationConfiguration
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.properties.UiProperties
import io.stereov.singularity.mail.template.service.TemplateService
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@AutoConfiguration(after = [
    ApplicationConfiguration::class
])
class TemplateConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun templateService(appProperties: AppProperties, uiProperties: UiProperties) = TemplateService(appProperties, uiProperties)

}
