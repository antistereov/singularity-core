package io.stereov.singularity.content.core.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class ContentAccessRoleException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?,
) : SingularityException(msg, code, status, description, cause) {

    class Invalid(input: String) : ContentAccessRoleException(
        "Invalid access role: $input",
        "INVALID_ACCESS_ROLE",
        HttpStatus.BAD_REQUEST,
        "Invalid access role.",
        null
    )
}
