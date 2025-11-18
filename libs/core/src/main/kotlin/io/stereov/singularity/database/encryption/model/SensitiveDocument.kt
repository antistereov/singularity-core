package io.stereov.singularity.database.encryption.model

/**
 * Represents a sensitive document that contains sensitive data of a specific type.
 *
 * The `SensitiveDocument` interface provides a structure for handling documents where some data
 * is considered sensitive. The exact type of sensitive data is determined by the generic parameter `S`.
 *
 * @param S The type of sensitive data contained in the document.
 * @property sensitive The sensitive data contained within the document.
 */
interface SensitiveDocument<S> {

    val sensitive: S
}
