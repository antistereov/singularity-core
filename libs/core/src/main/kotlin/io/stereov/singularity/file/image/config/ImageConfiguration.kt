package io.stereov.singularity.file.image.config

import com.sksamuel.scrimage.webp.WebpWriter
import io.stereov.singularity.file.core.component.DataBufferPublisher
import io.stereov.singularity.file.core.service.FileStorage
import io.stereov.singularity.file.image.properties.ImageProperties
import io.stereov.singularity.file.image.service.ImageStore
import io.stereov.singularity.global.config.ApplicationConfiguration
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@AutoConfiguration(
    after = [
        ApplicationConfiguration::class,
    ]
)
@EnableConfigurationProperties(ImageProperties::class)
class ImageConfiguration {

    @Bean
    fun webpWriter() = WebpWriter(4, 85, 4, false)

    @Bean
    fun losslessWebpWriter() = WebpWriter(6, 100, 6, true)

    // Service

    @Bean
    @ConditionalOnMissingBean
    fun imageStore(
        imageProperties: ImageProperties,
        webpWriter: WebpWriter,
        fileStorage: FileStorage,
        dataBufferPublisher: DataBufferPublisher
    ) = ImageStore(
        imageProperties,
        webpWriter,
        fileStorage,
        dataBufferPublisher
    )
}