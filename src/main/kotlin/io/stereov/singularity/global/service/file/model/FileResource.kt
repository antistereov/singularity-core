package io.stereov.singularity.global.service.file.model

import kotlinx.serialization.Serializable
import org.springframework.core.io.Resource

@Serializable
data class FileResource(
    val resource: Resource,
    val mediaType: String
)
