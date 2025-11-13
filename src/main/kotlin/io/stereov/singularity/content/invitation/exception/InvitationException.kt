package io.stereov.singularity.content.invitation.exception

import io.stereov.singularity.global.exception.SingularityException

open class InvitationException(msg: String, cause: Throwable? = null) : SingularityException(msg, cause)
