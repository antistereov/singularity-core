package io.stereov.singularity.core.properties.storage

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "baseline.file.storage")
data class StorageProperties(
    val type: StorageType = StorageType.S3
) {

}
