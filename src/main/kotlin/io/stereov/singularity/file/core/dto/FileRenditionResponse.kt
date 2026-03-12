package io.stereov.singularity.file.core.dto

import io.stereov.singularity.file.core.model.FileRenditionKey

data class FileRenditionResponse(
    val key: FileRenditionKey,
    val size: Long,
    val contentType: String,
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
)
