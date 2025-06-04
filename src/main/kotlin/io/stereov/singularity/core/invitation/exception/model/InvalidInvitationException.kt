package io.stereov.singularity.core.invitation.exception.model

import io.stereov.singularity.core.invitation.exception.InvitationException


class InvalidInvitationException(
    msg: String = "The invitation is either invalid or expired",
    cause: Throwable? = null
) : InvitationException(msg, cause)
