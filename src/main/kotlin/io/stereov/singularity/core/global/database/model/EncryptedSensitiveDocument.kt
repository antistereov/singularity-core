package io.stereov.singularity.core.global.database.model

import io.stereov.singularity.core.global.service.encryption.model.Encrypted
import org.bson.types.ObjectId

abstract class EncryptedSensitiveDocument<T> {

    abstract val _id: ObjectId?
    abstract val sensitive: Encrypted<T>
    abstract fun toSensitiveDocument(decrypted: T, otherValues: List<Any> = emptyList()): SensitiveDocument<T>
}
