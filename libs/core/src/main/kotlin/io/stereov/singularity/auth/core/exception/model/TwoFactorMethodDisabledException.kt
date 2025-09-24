package io.stereov.singularity.auth.core.exception.model

import io.stereov.singularity.auth.core.exception.AuthException
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod

class TwoFactorMethodDisabledException(method: TwoFactorMethod) : AuthException(message = "Two factor authentication via $method disabled for current user")
