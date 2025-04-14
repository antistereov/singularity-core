package io.stereov.web.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("baseline.file.storage.local")
data class LocalFileStorageProperties(
    val basePath: String = "file:/opt/app/uploads"
)
