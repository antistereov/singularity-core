package io.stereov.singularity.database.model

import io.stereov.singularity.encryption.model.Encrypted
import org.bson.types.ObjectId

interface EncryptedSensitiveDocument<T> {

    val _id: ObjectId?
    val sensitive: Encrypted<T>
    fun toSensitiveDocument(decrypted: T, otherValues: List<Any> = emptyList()): SensitiveDocument<T>
}
