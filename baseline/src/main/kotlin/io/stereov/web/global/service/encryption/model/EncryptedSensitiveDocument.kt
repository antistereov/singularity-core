package io.stereov.web.global.service.encryption.model

abstract class EncryptedSensitiveDocument<T>(
    val sensitiveData: Encrypted<T>
) {

    abstract fun toSensitiveDocument(decrypted: T, otherValues: List<Any> = emptyList()): SensitiveDocument<T>
}
