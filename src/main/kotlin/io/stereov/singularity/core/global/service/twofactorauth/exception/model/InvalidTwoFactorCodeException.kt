package io.stereov.singularity.core.global.service.twofactorauth.exception.model

import io.stereov.singularity.core.global.service.twofactorauth.exception.TwoFactorAuthException

class InvalidTwoFactorCodeException(msg: String = "Invalid 2FA code") : TwoFactorAuthException(msg)
