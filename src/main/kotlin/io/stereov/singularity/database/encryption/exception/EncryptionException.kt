package io.stereov.singularity.database.encryption.exception

import io.stereov.singularity.global.exception.SingularityException

sealed class EncryptionException(
    msg: String,
    code: String,
    cause: Throwable?
) : SingularityException(msg, code, cause) {

    class ObjectMapping(msg: String, cause: Throwable? = null) : EncryptionException(msg, CODE, cause) {
        companion object { const val CODE = "ENCRYPTION_OBJECT_MAPPING_FAILURE" }
    }
    class Cipher(msg: String, cause: Throwable? = null) : EncryptionException(msg, CODE, cause) {
        companion object { const val CODE = "ENCRYPTION_CIPHER_FAILURE" }
    }
    class Secret(msg: String, cause: Throwable? = null) : EncryptionException(msg, CODE, cause) {
        companion object { const val CODE = "ENCRYPTION_SECRET_FAILURE" }
    }
    class Encoding(msg: String, cause: Throwable? = null) : EncryptionException(msg, CODE, cause) {
        companion object { const val CODE = "ENCRYPTION_ENCODING_FAILURE" }
    }
}