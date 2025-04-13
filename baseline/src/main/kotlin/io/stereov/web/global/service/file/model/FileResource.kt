package io.stereov.web.global.service.file.model

import org.springframework.core.io.Resource
import org.springframework.http.MediaType

data class FileResource(
    val resource: Resource,
    val mediaType: MediaType
)
