package io.stereov.web.auth.exception

class NoTwoFactorUserAttributeException : AuthException(
    message = "No two factor authentication user attribute found in request"
)
