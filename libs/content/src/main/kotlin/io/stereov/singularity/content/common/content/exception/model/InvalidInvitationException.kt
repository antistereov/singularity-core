package io.stereov.singularity.content.common.content.exception.model

import io.stereov.singularity.content.common.content.exception.ContentException

class InvalidInvitationException(msg: String, cause: Throwable? = null) : ContentException(msg, cause)
