package io.stereov.web.global.service.twofactorauth.exception

import io.stereov.web.global.exception.BaseWebException

open class TwoFactorAuthException(message: String, cause: Throwable? = null) : BaseWebException(message, cause) {
}
