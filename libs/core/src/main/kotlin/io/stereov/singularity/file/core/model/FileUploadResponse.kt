package io.stereov.singularity.file.core.model

data class FileUploadResponse(
    val key: String,
    val size: Long,
    val contentType: String,
    val width: Int?,
    val height: Int?
)
