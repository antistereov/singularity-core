package io.stereov.singularity.global.config

import io.stereov.singularity.global.exception.SingularityExceptionHandler
import io.stereov.singularity.global.properties.AppProperties
import io.stereov.singularity.global.properties.UiProperties
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@AutoConfiguration
@EnableConfigurationProperties(
    AppProperties::class,
    UiProperties::class,
)
@EnableScheduling
class ApplicationConfiguration {

    // Exception Handler

    @Bean
    @ConditionalOnMissingBean
    fun singularityExceptionHandler() = SingularityExceptionHandler()

}
