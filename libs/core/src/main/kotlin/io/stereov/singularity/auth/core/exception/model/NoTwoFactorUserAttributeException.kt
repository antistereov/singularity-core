package io.stereov.singularity.auth.core.exception.model

import io.stereov.singularity.auth.core.exception.AuthenticationException

/**
 * # Exception thrown when no two-factor authentication user attribute is found in the request.
 *
 * This exception is used to indicate that the request does not contain a two-factor authentication user attribute,
 * which is required for two-factor authentication. It extends the [AuthenticationException] class.
 *
 * @author <a href="https://github.com/antistereov">antistereov</a>
 */
class NoTwoFactorUserAttributeException : AuthenticationException(
    msg = "No two factor authentication user attribute found in request"
)
