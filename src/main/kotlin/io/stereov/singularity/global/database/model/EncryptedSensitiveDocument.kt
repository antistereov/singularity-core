package io.stereov.singularity.global.database.model

import io.stereov.singularity.global.service.encryption.model.Encrypted

abstract class EncryptedSensitiveDocument<T> {

    abstract val _id: String?
    abstract val sensitive: Encrypted<T>
    abstract fun toSensitiveDocument(decrypted: T, otherValues: List<Any> = emptyList()): SensitiveDocument<T>
}
