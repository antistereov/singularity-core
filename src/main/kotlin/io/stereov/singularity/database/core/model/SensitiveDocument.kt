package io.stereov.singularity.database.core.model

import io.stereov.singularity.database.encryption.model.Encrypted

interface SensitiveDocument<S> {

    val sensitive: S
    fun toEncryptedDocument(encryptedSensitiveData: Encrypted<S>, otherValues: List<Any>): EncryptedSensitiveDocument<S>
}
