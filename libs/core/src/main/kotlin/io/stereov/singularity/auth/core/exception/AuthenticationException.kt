package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.global.exception.SingularityException

sealed class AuthenticationException(
    msg: String,
    code: String,
    cause: Throwable? = null
) : SingularityException(msg, code, cause) {

    class RoleRequired(msg: String, cause: Throwable? = null) : AuthenticationException(msg, "ROLE_REQUIRED", cause)
    class GroupMembershipRequired(msg: String, cause: Throwable? = null) : AuthenticationException(msg, "GROUP_MEMBERSHIP_REQUIRED", cause)
}
