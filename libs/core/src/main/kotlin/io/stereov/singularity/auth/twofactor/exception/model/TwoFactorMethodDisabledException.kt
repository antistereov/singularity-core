package io.stereov.singularity.auth.twofactor.exception.model

import io.stereov.singularity.auth.twofactor.exception.TwoFactorAuthException

class TwoFactorMethodDisabledException(msg: String, cause: Throwable? = null) : TwoFactorAuthException(msg, cause)