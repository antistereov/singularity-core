package io.stereov.singularity.global.exception

import org.springframework.http.HttpStatus

object ConfigurationFailure {
    const val CODE = "CONFIGURATION_FAILURE"
    const val DESCRIPTION = "Failure due to invalid configuration"
    val STATUS = HttpStatus.INTERNAL_SERVER_ERROR
}