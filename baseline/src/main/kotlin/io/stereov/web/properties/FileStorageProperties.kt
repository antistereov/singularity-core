package io.stereov.web.properties

import io.stereov.web.global.service.file.model.StorageType
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "baseline.file.storage")
data class FileStorageProperties(
    val type: StorageType = StorageType.LOCAL,
)
