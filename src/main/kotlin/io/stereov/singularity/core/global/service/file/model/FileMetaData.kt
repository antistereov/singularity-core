package io.stereov.singularity.core.global.service.file.model

import io.stereov.singularity.core.auth.model.AccessType
import java.time.Instant

data class FileMetaData(
    val key: String,
    val contentType: String,
    val accessType: AccessType,
    val publicUrl: String?,
    val size: Long,
    val uploaded: Instant = Instant.now()
)
