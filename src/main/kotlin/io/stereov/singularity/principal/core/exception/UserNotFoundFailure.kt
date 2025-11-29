package io.stereov.singularity.principal.core.exception

import org.springframework.http.HttpStatus

object UserNotFoundFailure {
    const val CODE = "USER_NOT_FOUND"
    const val DESCRIPTION = "User not found."
    val STATUS = HttpStatus.NOT_FOUND
}