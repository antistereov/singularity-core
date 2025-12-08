package io.stereov.singularity.global.exception

import org.springframework.http.HttpStatus

object ResponseMappingFailure {
    const val CODE = "RESPONSE_MAPPING_FAILURE"
    const val DESCRIPTION = "Response mapping failure."
    val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
}