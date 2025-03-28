package io.stereov.web.global.service.twofactorauth.exception.model

import io.stereov.web.global.service.twofactorauth.exception.TwoFactorAuthException

class InvalidTwoFactorCodeException() : TwoFactorAuthException(message = "Invalid 2FA code")
