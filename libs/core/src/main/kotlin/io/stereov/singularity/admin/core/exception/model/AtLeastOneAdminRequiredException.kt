package io.stereov.singularity.admin.core.exception.model

import io.stereov.singularity.admin.core.exception.AdminException

class AtLeastOneAdminRequiredException(msg: String, cause: Throwable? = null) : AdminException(msg, cause)