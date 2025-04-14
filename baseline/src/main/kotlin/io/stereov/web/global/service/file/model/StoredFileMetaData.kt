package io.stereov.web.global.service.file.model

import kotlinx.serialization.Serializable
import org.springframework.http.MediaType
import org.springframework.http.codec.multipart.FilePart

@Serializable
data class StoredFileMetaData(
    val filename: String,
    val subfolder: String,
    val mediaType: String,
) {

    constructor(filename: String, subfolder: String, file: FilePart) : this(
        filename,
        subfolder,
        (file.headers().contentType ?: MediaType.APPLICATION_OCTET_STREAM).toString()
    )
}
