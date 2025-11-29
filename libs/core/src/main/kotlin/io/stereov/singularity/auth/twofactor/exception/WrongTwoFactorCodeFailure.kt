package io.stereov.singularity.auth.twofactor.exception

import org.springframework.http.HttpStatus

object WrongTwoFactorCodeFailure {
    const val CODE = "WRONG_TWO_FACTOR_CODE"
    const val DESCRIPTION = "Wrong two-factor code."
    val STATUS = HttpStatus.UNAUTHORIZED
}