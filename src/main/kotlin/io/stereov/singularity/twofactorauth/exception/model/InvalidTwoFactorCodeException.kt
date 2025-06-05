package io.stereov.singularity.twofactorauth.exception.model

import io.stereov.singularity.twofactorauth.exception.TwoFactorAuthException

class InvalidTwoFactorCodeException(msg: String = "Invalid 2FA code") : TwoFactorAuthException(msg)
