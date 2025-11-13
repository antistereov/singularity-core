package io.stereov.singularity.auth.twofactor.exception

import io.stereov.singularity.global.exception.SingularityException

open class TwoFactorAuthException(message: String, cause: Throwable? = null) : SingularityException(message, cause) {
}
