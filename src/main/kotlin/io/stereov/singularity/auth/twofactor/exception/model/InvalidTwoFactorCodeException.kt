package io.stereov.singularity.auth.twofactor.exception.model

import io.stereov.singularity.auth.twofactor.exception.TwoFactorAuthException

class InvalidTwoFactorCodeException(msg: String = "Invalid 2FA code") : TwoFactorAuthException(msg)
