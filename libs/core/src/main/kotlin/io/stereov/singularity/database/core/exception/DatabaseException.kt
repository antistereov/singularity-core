package io.stereov.singularity.database.core.exception

import io.stereov.singularity.global.exception.SingularityException

open class DatabaseException(msg: String, cause: Throwable? = null) : SingularityException(msg, cause)
