package io.stereov.singularity.auth.core.exception.model

import io.stereov.singularity.auth.core.exception.AuthenticationException
import io.stereov.singularity.auth.twofactor.model.TwoFactorMethod

class TwoFactorMethodDisabledException(method: TwoFactorMethod) : AuthenticationException(msg = "Two factor authentication via $method disabled for current user")
