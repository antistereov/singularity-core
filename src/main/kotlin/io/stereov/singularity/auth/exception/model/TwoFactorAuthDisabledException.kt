package io.stereov.singularity.auth.exception.model

import io.stereov.singularity.auth.exception.AuthException

class TwoFactorAuthDisabledException() : AuthException(message = "Two factor authentication is disabled for current user")
