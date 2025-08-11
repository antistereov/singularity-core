package io.stereov.singularity.content.invitation.exception.model

import io.stereov.singularity.content.invitation.exception.InvitationException


class InvalidInvitationException(
    msg: String = "The invitation is either invalid or expired",
    cause: Throwable? = null
) : InvitationException(msg, cause)
