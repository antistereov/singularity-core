package io.stereov.singularity.file.core.model

import io.stereov.singularity.auth.model.AccessType
import java.time.Instant

data class FileMetaData(
    val key: String,
    val contentType: String,
    val accessType: AccessType,
    val publicUrl: String?,
    val size: Long,
    val uploaded: Instant = Instant.now()
)
