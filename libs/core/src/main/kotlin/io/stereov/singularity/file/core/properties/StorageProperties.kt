package io.stereov.singularity.file.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "singularity.file.storage")
data class StorageProperties(
    val type: StorageType = StorageType.S3
)
