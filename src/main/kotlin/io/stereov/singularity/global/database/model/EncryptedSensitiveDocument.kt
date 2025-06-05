package io.stereov.singularity.global.database.model

import io.stereov.singularity.global.service.encryption.model.Encrypted
import org.bson.types.ObjectId

interface EncryptedSensitiveDocument<T> {

    val _id: ObjectId?
    val sensitive: Encrypted<T>
    fun toSensitiveDocument(decrypted: T, otherValues: List<Any> = emptyList()): SensitiveDocument<T>
}
