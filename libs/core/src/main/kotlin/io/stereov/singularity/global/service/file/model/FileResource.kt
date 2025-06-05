package io.stereov.singularity.global.service.file.model

import org.springframework.core.io.Resource

data class FileResource(
    val resource: Resource,
    val mediaType: String
)
