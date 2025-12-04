package io.stereov.singularity.database.encryption.model

import io.stereov.singularity.database.core.model.WithId
import org.bson.types.ObjectId

/**
 * Represents a sensitive document with encrypted sensitive data.
 *
 * The `EncryptedSensitiveDocument` interface defines a structure for managing sensitive documents
 * where the sensitive content is encrypted. It combines an identifier for the document with the
 * encrypted sensitive data that is stored in the form of an `Encrypted` object.
 *
 * @param T The type of the sensitive data being encrypted and stored within the document.
 * @property _id The unique identifier for the document, represented as an optional `ObjectId`.
 * @property sensitive The encrypted sensitive data contained in the document, represented as an `Encrypted` object.
 */
interface EncryptedSensitiveDocument<T> : WithId {

    override val _id: ObjectId?
    val sensitive: Encrypted<T>
}
