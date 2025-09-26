package io.stereov.singularity.file.image.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.file.storage.image")
data class ImageProperties(
    val small: Int = 400,
    val medium: Int = 800,
    val large: Int = 1920,
    val storeOriginal: Boolean = true,
)
