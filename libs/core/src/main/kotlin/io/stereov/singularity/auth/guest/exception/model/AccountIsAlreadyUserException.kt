package io.stereov.singularity.auth.guest.exception.model

import io.stereov.singularity.auth.guest.exception.GuestException

class AccountIsAlreadyUserException(msg: String, cause: Throwable? = null) : GuestException(msg, cause) {
}