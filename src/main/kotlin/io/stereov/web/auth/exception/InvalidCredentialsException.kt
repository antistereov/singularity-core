package io.stereov.web.auth.exception

class InvalidCredentialsException : AuthException(
    message = "Login failed: Invalid credentials",
)
