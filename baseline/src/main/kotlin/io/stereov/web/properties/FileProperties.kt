package io.stereov.web.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "baseline.file")
data class FileProperties(
    val basePath: String = "file:/opt/app/uploads"
)
