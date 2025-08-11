package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.global.exception.BaseWebException

open class TwoFactorAuthException(message: String, cause: Throwable? = null) : BaseWebException(message, cause) {
}
