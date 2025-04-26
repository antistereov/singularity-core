package io.stereov.singularity.core.global.service.twofactorauth.exception

import io.stereov.singularity.core.global.exception.BaseWebException

open class TwoFactorAuthException(message: String, cause: Throwable? = null) : BaseWebException(message, cause) {
}
