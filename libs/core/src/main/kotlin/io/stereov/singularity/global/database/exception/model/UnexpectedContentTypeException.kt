package io.stereov.singularity.global.database.exception.model

import io.stereov.singularity.global.database.exception.DatabaseException

class UnexpectedContentTypeException(contentClass: Class<*>, cause: Throwable) : DatabaseException(
    msg = "Query failed because content type does not match: ${contentClass.simpleName}",
    cause = cause
)
