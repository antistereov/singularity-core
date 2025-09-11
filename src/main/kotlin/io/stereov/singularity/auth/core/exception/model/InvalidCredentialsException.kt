package io.stereov.singularity.auth.core.exception.model

import io.stereov.singularity.auth.core.exception.AuthException

/**
 * # Exception thrown when login fails due to invalid credentials.
 *
 * This exception is used to indicate that the provided credentials are incorrect or invalid.
 *
 * @property message The error message to be displayed.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class InvalidCredentialsException(msg: String = "Authentication failed: Invalid credentials") : AuthException(msg)
