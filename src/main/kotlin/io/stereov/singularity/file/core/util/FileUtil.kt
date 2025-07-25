package io.stereov.singularity.file.core.util

import io.stereov.singularity.file.core.model.FileResource
import org.springframework.core.io.UrlResource
import org.springframework.http.MediaType
import java.io.File
import java.nio.file.Files

fun File.toResource(): FileResource {
    val resource = UrlResource(this.toURI())

    val mimeType = Files.probeContentType(this.toPath())
        ?: "application/octet-stream"
    val mediaType = MediaType.parseMediaType(mimeType)

    return FileResource(resource, mediaType.type)
}
