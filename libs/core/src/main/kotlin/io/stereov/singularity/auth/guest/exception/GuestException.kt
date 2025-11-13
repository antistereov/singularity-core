package io.stereov.singularity.auth.guest.exception

import io.stereov.singularity.global.exception.SingularityException

open class GuestException(msg: String, cause: Throwable? = null) : SingularityException(msg, cause)