package io.stereov.singularity.database.encryption.model

import io.stereov.singularity.database.core.model.WithId

/**
 * Represents a sensitive document that contains sensitive data of a specific type.
 *
 * The `SensitiveDocument` interface provides a structure for handling documents where some data
 * is considered sensitive. The exact type of sensitive data is determined by the generic parameter `S`.
 *
 * @param S The type of sensitive data contained in the document.
 * @property sensitive The sensitive data contained within the document.
 */
interface SensitiveDocument<S> : WithId {

    val sensitive: S
}
