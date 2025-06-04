package io.stereov.singularity.core.invitation.exception.model

import io.stereov.singularity.core.invitation.exception.InvitationException


class InvalidInvitationException(cause: Throwable? = null) : InvitationException(
    msg = "The invitation is either invalid or expired", cause
)
