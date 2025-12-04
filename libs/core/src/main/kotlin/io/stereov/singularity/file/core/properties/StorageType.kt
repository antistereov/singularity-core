package io.stereov.singularity.file.core.properties

/**
 * Represents the type of storage system to be used for file management.
 *
 * This enum is utilized to define the storage backend, such as S3 or Local files.
 * It is typically configured in application properties and used in the application
 * to determine how file storage operations should be handled.
 *
 * Possible values:
 * - S3: Refers to Amazon Simple Storage Service or a compatible S3-based storage service.
 * - LOCAL: Refers to local filesystem storage.
 */
enum class StorageType {
    S3,
    LOCAL
}
