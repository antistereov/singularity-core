package io.stereov.web.auth.exception.model

import io.stereov.web.auth.exception.AuthException

class TwoFactorAuthDisabledException() : AuthException(message = "Two factor authentication is disabled for current user")
