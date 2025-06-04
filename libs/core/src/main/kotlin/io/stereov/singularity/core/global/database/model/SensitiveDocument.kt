package io.stereov.singularity.core.global.database.model

import io.stereov.singularity.core.global.service.encryption.model.Encrypted

interface SensitiveDocument<S> {

    val sensitive: S
    fun toEncryptedDocument(encryptedSensitiveData: Encrypted<S>, otherValues: List<Any>): EncryptedSensitiveDocument<S>
}
