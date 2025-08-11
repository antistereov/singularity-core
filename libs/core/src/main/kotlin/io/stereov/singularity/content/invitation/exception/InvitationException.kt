package io.stereov.singularity.content.invitation.exception

import io.stereov.singularity.global.exception.BaseWebException

open class InvitationException(msg: String, cause: Throwable? = null) : BaseWebException(msg, cause)
