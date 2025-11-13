package io.stereov.singularity.secrets.core.exception

import io.stereov.singularity.global.exception.SingularityException

sealed class SecretStoreException(
    msg: String,
    code: String,
    cause: Throwable?
) : SingularityException(msg, code, cause) {

    class NotFound(msg: String, cause: Throwable? = null) : SecretStoreException(msg, CODE, cause) {
        companion object { const val CODE = "SECRET_NOT_FOUND" }
    }
    class KeyGenerator(msg: String, cause: Throwable? = null) : SecretStoreException(msg, CODE, cause) {
        companion object { const val CODE = "SECRET_KEY_GENERATION_FAILURE"}
    }
    class Operation(msg: String, cause: Throwable? = null) : SecretStoreException(msg, CODE, cause) {
        companion object { const val CODE = "SECRET_OPERATION_FAILURE"}
    }
}