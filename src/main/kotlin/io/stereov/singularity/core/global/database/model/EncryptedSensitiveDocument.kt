package io.stereov.singularity.core.global.database.model

import io.stereov.singularity.core.global.service.encryption.model.Encrypted
import org.bson.types.ObjectId

interface EncryptedSensitiveDocument<T> {

    val _id: ObjectId?
    val sensitive: Encrypted<T>
    fun toSensitiveDocument(decrypted: T, otherValues: List<Any> = emptyList()): SensitiveDocument<T>
}
