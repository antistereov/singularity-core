package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class AuthenticationException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable? = null
) : SingularityException(msg, code, status, description, cause) {

    class AuthenticationRequired(msg: String, cause: Throwable? = null) : AuthenticationException(
        msg,
        "AUTHENTICATION_REQUIRED",
        HttpStatus.UNAUTHORIZED,
        "User is not authenticated.",
        cause
    )

    class RoleRequired(msg: String, cause: Throwable? = null) : AuthenticationException(
        msg,
        "ROLE_REQUIRED",
        HttpStatus.FORBIDDEN,
        "User does not have required role.",
        cause
    )

    class GroupMembershipRequired(msg: String, cause: Throwable? = null) : AuthenticationException(
        msg,
        "GROUP_MEMBERSHIP_REQUIRED",
        HttpStatus.FORBIDDEN,
        "User is not a member of required group.",
        cause
    )

    class AlreadyAuthenticated(msg: String, cause: Throwable? = null) : AuthenticationException(
        msg,
        "ALREADY_AUTHENTICATED",
        HttpStatus.NOT_MODIFIED,
        "User is already authenticated.",
        cause
    )
}
