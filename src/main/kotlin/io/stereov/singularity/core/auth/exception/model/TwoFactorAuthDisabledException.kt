package io.stereov.singularity.core.auth.exception.model

import io.stereov.singularity.core.auth.exception.AuthException

class TwoFactorAuthDisabledException() : AuthException(message = "Two factor authentication is disabled for current user")
