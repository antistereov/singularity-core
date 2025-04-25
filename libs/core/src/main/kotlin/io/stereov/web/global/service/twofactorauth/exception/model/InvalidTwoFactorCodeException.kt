package io.stereov.web.global.service.twofactorauth.exception.model

import io.stereov.web.global.service.twofactorauth.exception.TwoFactorAuthException

class InvalidTwoFactorCodeException(msg: String = "Invalid 2FA code") : TwoFactorAuthException(msg)
