package io.stereov.singularity.file.core.model

data class FileRendition(
    val key: String,
    val size: Long,
    val contentType: String,
    val height: Int? = null,
    val width: Int? = null,
)