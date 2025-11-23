package io.stereov.singularity.auth.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class AuthenticationException(
    msg: String,
    code: String,
    status: HttpStatus,
    cause: Throwable? = null
) : SingularityException(msg, code, status, cause) {

    class AuthenticationRequired(
        msg: String,
        cause: Throwable? = null
    ) : AuthenticationException(msg, CODE, STATUS, cause) {
        companion object {
            const val CODE = "AUTHENTICATION_REQUIRED"
            val STATUS = HttpStatus.UNAUTHORIZED
        }
    }

    class RoleRequired(
        msg: String,
        cause: Throwable? = null
    ) : AuthenticationException(msg, CODE,  STATUS, cause) {
        companion object {
            const val CODE = "ROLE_REQUIRED"
            val STATUS = HttpStatus.FORBIDDEN
        }
    }

    class GroupMembershipRequired(
        msg: String,
        cause: Throwable? = null
    ) : AuthenticationException(msg, CODE, STATUS, cause) {

        companion object {
            const val CODE = "GROUP_MEMBERSHIP_REQUIRED"
            val STATUS = HttpStatus.FORBIDDEN
        }
    }
}
