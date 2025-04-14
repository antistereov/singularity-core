package io.stereov.web.global.service.file.model

import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

/**
 * This class contains an [InputStreamResource] of a stored file.
 */
data class StoredFile(
    val resource: InputStreamResource,
    val filename: String,
    val subfolder: String,
    val mediaType: MediaType,
) {
    constructor(resource: InputStreamResource, file: StoredFileMetaData): this(
        resource,
        file.filename,
        file.subfolder,
        MediaType.parseMediaType(file.mediaType)
    )

    fun toResponseEntity(): ResponseEntity<InputStreamResource> {
        return ResponseEntity.ok()
            .contentType(mediaType)
            .body(resource)
    }
}
