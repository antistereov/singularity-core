package io.stereov.singularity.global.database.model

import io.stereov.singularity.encryption.model.Encrypted

interface SensitiveDocument<S> {

    val sensitive: S
    fun toEncryptedDocument(encryptedSensitiveData: Encrypted<S>, otherValues: List<Any>): EncryptedSensitiveDocument<S>
}
