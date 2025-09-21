package io.stereov.singularity.auth.guest.exception.model

import io.stereov.singularity.auth.guest.exception.GuestException

class GuestCannotPerformThisActionException(msg: String, cause: Throwable? = null) : GuestException(msg, cause)
