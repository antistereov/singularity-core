package io.stereov.web.global.service.encryption.model

abstract class SensitiveDocument<S>(
    open val sensitive: S
) {

    abstract fun toEncryptedDocument(encryptedSensitiveData: Encrypted<S>, otherValues: List<Any>): EncryptedSensitiveDocument<S>
}
