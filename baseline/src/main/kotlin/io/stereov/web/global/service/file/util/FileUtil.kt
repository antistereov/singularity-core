package io.stereov.web.global.service.file.util

import io.stereov.web.global.service.file.model.FileResource
import org.springframework.core.io.UrlResource
import org.springframework.http.MediaType
import java.io.File
import java.nio.file.Files

fun File.toResource(): FileResource {
    val resource = UrlResource(this.toURI())

    val mimeType = Files.probeContentType(this.toPath())
        ?: "application/octet-stream"
    val mediaType = MediaType.parseMediaType(mimeType)

    return FileResource(resource, mediaType)
}
