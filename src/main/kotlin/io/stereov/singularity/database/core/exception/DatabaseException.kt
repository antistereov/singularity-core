package io.stereov.singularity.database.core.exception

import io.stereov.singularity.global.exception.BaseWebException

open class DatabaseException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
