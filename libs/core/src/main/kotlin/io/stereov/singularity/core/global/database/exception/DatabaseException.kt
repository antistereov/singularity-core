package io.stereov.singularity.core.global.database.exception

import io.stereov.singularity.core.global.exception.BaseWebException

open class DatabaseException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
