package io.stereov.singularity.file.core.dto

data class FileRenditionResponse(
    val key: String,
    val size: Long,
    val contentType: String,
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
)
