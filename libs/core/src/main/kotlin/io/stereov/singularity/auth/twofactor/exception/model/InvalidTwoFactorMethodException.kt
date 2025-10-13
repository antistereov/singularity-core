package io.stereov.singularity.auth.twofactor.exception.model

import io.stereov.singularity.auth.twofactor.exception.TwoFactorAuthException

class InvalidTwoFactorMethodException(type: String) : TwoFactorAuthException(message = "No two factor method $type exists.")