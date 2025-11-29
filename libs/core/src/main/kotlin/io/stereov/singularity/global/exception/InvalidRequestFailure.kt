package io.stereov.singularity.global.exception

import org.springframework.http.HttpStatus

object InvalidRequestFailure {
    const val CODE = "INVALID_REQUEST"
    const val DESCRIPTION = "Invalid request."
    val STATUS = HttpStatus.BAD_REQUEST
}