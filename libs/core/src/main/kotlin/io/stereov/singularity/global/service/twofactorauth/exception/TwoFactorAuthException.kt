package io.stereov.singularity.global.service.twofactorauth.exception

import io.stereov.singularity.global.exception.BaseWebException

open class TwoFactorAuthException(message: String, cause: Throwable? = null) : BaseWebException(message, cause) {
}
