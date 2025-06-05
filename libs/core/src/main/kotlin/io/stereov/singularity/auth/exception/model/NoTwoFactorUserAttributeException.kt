package io.stereov.singularity.auth.exception.model

import io.stereov.singularity.auth.exception.AuthException

/**
 * # Exception thrown when no two-factor authentication user attribute is found in the request.
 *
 * This exception is used to indicate that the request does not contain a two-factor authentication user attribute,
 * which is required for two-factor authentication. It extends the [AuthException] class.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class NoTwoFactorUserAttributeException : AuthException(
    message = "No two factor authentication user attribute found in request"
)
