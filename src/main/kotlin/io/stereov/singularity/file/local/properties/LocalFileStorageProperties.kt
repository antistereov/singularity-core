package io.stereov.singularity.file.local.properties

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("singularity.file.storage.local")
@ConditionalOnProperty(prefix = "singularity.file.storage", value = ["type"], havingValue = "local", matchIfMissing = true)
data class LocalFileStorageProperties(
    val fileDirectory: String = "./.data/files",
)
