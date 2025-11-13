package io.stereov.singularity.cache.exception

import io.stereov.singularity.global.exception.SingularityException

sealed class CacheException(
    msg: String,
    code: String,
    cause: Throwable?
) : SingularityException(msg, code, cause) {

    class ObjectMapper(msg: String, cause: Throwable? = null) : CacheException(msg, CODE, cause) {
        companion object { const val CODE = "CACHE_OBJECT_MAPPING_FAILURE" }
    }
    class Operation(msg: String, cause: Throwable? = null) : CacheException(msg, CODE, cause) {
        companion object { const val CODE = "CACHE_OPERATION_FAILURE" }
    }
    class KeyNotFound(msg: String, cause: Throwable? = null) : CacheException(msg, CODE, cause) {
        companion object { const val CODE = "CACHE_KEY_NOT_FOUND" }
    }
}
