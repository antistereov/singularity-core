package io.stereov.singularity.invitation.exception.model

import io.stereov.singularity.invitation.exception.InvitationException


class InvalidInvitationException(
    msg: String = "The invitation is either invalid or expired",
    cause: Throwable? = null
) : InvitationException(msg, cause)
