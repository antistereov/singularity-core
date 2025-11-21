package io.stereov.singularity.file.core.model

/**
 * Represents a specific rendition of a file, providing details such as its unique key,
 * size, content type, and optional dimensions.
 *
 * This data class is used to define different representations of a file, potentially with
 * varying resolutions, sizes, or formats.
 * Renditions are identified by a unique key and are
 * associated with their respective metadata, including size and MIME type. Optional width and
 * height can be provided for renditions that have physical dimensions (e.g., images or videos).
 *
 * @property key A unique identifier for the file rendition.
 * @property size The size of the file rendition in bytes.
 * @property contentType The MIME type of the file rendition (e.g., image/png, video/mp4).
 * @property height The height of the file rendition in pixels, if applicable.
 * @property width The width of the file rendition in pixels, if applicable.
 */
data class FileRendition(
    val key: String,
    val size: Long,
    val contentType: String,
    val height: Int? = null,
    val width: Int? = null,
)
