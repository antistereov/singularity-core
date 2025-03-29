package io.stereov.web.user.exception.model

import io.stereov.web.user.exception.UserException

class NoAppInfoFoundException(userId: String) : UserException(
    message = "No application info found for user $userId"
)
