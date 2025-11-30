package io.stereov.singularity.principal.core.exception

import io.stereov.singularity.database.core.exception.DatabaseFailure
import io.stereov.singularity.database.encryption.exception.DatabaseEncryptionFailure
import io.stereov.singularity.database.encryption.exception.FindAllEncryptedDocumentsPaginatedException
import io.stereov.singularity.database.hash.exception.HashFailure
import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class GetUsersException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable?
) : SingularityException(msg, code, status, description, cause) {

    class Database(msg: String, cause: Throwable? = null) : GetUsersException(
        msg,
        DatabaseFailure.CODE,
        DatabaseFailure.STATUS,
        DatabaseFailure.DESCRIPTION,
        cause
    )

    class Encryption(msg: String, cause: Throwable? = null) : GetUsersException(
        msg,
        DatabaseEncryptionFailure.CODE,
        DatabaseEncryptionFailure.STATUS,
        DatabaseEncryptionFailure.DESCRIPTION,
        cause
    )

    class Hash(msg: String, cause: Throwable? = null) : GetUsersException(
        msg,
        HashFailure.CODE,
        HashFailure.STATUS,
        HashFailure.DESCRIPTION,
        cause
    )

    companion object {
        fun from(ex: FindAllEncryptedDocumentsPaginatedException) = when (ex) {
            is FindAllEncryptedDocumentsPaginatedException.Database -> Database(ex.message, ex.cause)
            is FindAllEncryptedDocumentsPaginatedException.Encryption -> Encryption(ex.message, ex.cause)
        }
    }

}