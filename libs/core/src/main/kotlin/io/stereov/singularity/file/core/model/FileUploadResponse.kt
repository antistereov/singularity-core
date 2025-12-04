package io.stereov.singularity.file.core.model

/**
 * Represents the response model for a file upload operation, including file metadata such as key, size,
 * MIME type, and optional dimensions.
 *
 * This class encapsulates details about an uploaded file and serves as the result of a successful upload.
 * The file metadata provides information that can be used for file identification, storage, and further
 * processing.
 *
 * @property key A unique identifier for the uploaded file.
 * @property size The size of the uploaded file in bytes.
 * @property contentType The MIME type of the uploaded file (e.g., image/png, application/pdf).
 * @property width The width of the uploaded file in pixels, if applicable (e.g., for images or videos).
 * @property height The height of the uploaded file in pixels, if applicable (e.g., for images or videos).
 */
data class FileUploadResponse(
    val key: String,
    val size: Long,
    val contentType: String,
    val width: Int?,
    val height: Int?
)
