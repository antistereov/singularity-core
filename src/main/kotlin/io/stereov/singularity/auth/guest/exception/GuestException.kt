package io.stereov.singularity.auth.guest.exception

import io.stereov.singularity.global.exception.BaseWebException

open class GuestException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)