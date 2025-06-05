package io.stereov.singularity.global.database.exception

import io.stereov.singularity.global.exception.BaseWebException

open class DatabaseException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
