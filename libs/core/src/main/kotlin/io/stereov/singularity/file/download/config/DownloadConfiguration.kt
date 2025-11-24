package io.stereov.singularity.file.download.config

import io.stereov.singularity.file.download.service.DownloadService
import io.stereov.singularity.global.config.ApplicationConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
    ]
)
class DownloadConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun downloadService(webClient: WebClient) = DownloadService(webClient)
}