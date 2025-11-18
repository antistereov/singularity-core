package io.stereov.singularity.database.encryption.repository

import io.stereov.singularity.database.encryption.model.EncryptedSensitiveDocument
import org.bson.types.ObjectId
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * A specialized repository interface extending `CoroutineCrudRepository` to manage sensitive documents
 * with encrypted content in a MongoDB database.
 *
 * The `SensitiveCrudRepository` is designed to work with documents that implement the
 * `EncryptedSensitiveDocument` interface. It provides CRUD functionality while ensuring sensitive
 * data is properly handled and stored in encrypted form.
 *
 * @param T The type of the entity being managed by the repository, which must extend `EncryptedSensitiveDocument`.
 */
interface SensitiveCrudRepository<T: EncryptedSensitiveDocument<*>> : CoroutineCrudRepository<T, ObjectId>
