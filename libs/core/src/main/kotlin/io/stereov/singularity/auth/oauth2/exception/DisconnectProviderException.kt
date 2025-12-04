package io.stereov.singularity.auth.oauth2.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.core.exception.PostCommitSideEffectFailure
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
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class PostCommitSideEffect(msg: String, cause: Throwable? = null) : DisconnectProviderException(
        msg,
        PostCommitSideEffectFailure.CODE,
        PostCommitSideEffectFailure.STATUS,
        PostCommitSideEffectFailure.DESCRIPTION,
        cause
    )
}