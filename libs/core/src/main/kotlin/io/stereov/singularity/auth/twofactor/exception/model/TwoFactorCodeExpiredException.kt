package io.stereov.singularity.auth.twofactor.exception.model

import io.stereov.singularity.auth.twofactor.exception.TwoFactorAuthException

class TwoFactorCodeExpiredException(msg: String) : TwoFactorAuthException(msg)
