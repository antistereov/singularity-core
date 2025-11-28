package io.stereov.singularity.auth.oauth2.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class DisconnectProviderException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class CannotDisconnectPassword(msg: String, cause: Throwable? = null) : DisconnectProviderException(
        msg,
        "CANNOT_DISCONNECT_PASSWORD",
        HttpStatus.BAD_REQUEST,
        "Cannot disconnect password provider.",
        cause
    )

    class ProviderNotFound(msg: String, cause: Throwable? = null) : DisconnectProviderException(
        msg,
        "PROVIDER_NOT_FOUND",
        HttpStatus.NOT_FOUND,
        "Provider not found.",
        cause
    )

    class CannotDisconnectLastProvider(msg: String, cause: Throwable? = null) : DisconnectProviderException(
        msg,
        "CANNOT_DISCONNECT_LAST_PROVIDER",
        HttpStatus.BAD_REQUEST,
        "Cannot disconnect last provider.",
        cause
    )

    class Database(msg: String, cause: Throwable? = null) : DisconnectProviderException(
        msg,
        "DATABASE_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to delete provider from database.",
        cause
    )

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : DisconnectProviderException(
        msg,
        "POST_COMMIT_SIDE_EFFECT_FAILURE",
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Failed to execute post commit side effect.",
        cause
    )
}