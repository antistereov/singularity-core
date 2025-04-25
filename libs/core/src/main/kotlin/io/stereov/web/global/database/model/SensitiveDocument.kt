package io.stereov.web.global.database.model

import io.stereov.web.global.service.encryption.model.Encrypted

abstract class SensitiveDocument<S> {

    abstract val sensitive: S
    abstract fun toEncryptedDocument(encryptedSensitiveData: Encrypted<S>, otherValues: List<Any>): EncryptedSensitiveDocument<S>
}
