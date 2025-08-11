package io.stereov.singularity.auth.core.exception.model

import io.stereov.singularity.auth.core.exception.AuthException

class TwoFactorAuthDisabledException() : AuthException(message = "Two factor authentication is disabled for current user")
