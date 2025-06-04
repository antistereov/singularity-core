package io.stereov.singularity.core.invitation.exception

import io.stereov.singularity.core.global.exception.BaseWebException

open class InvitationException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
