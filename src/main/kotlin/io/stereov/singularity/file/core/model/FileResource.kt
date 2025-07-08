package io.stereov.singularity.file.core.model

import org.springframework.core.io.Resource

data class FileResource(
    val resource: Resource,
    val mediaType: String
)
