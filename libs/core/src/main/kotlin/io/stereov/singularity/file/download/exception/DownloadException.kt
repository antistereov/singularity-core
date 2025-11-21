package io.stereov.singularity.file.download.exception

import io.stereov.singularity.global.exception.SingularityException

/**
 * Exception representing a failure during file download operations.
 *
 * This exception is thrown when there is an error while streaming or retrieving a file
 * from a specified URL. It is used to encapsulate the error message, code, and the underlying
 * cause of the failure.
 *
 * @constructor Creates an instance of [DownloadException].
 * @param msg The error message providing details about the failure.
 * @param cause The underlying cause of the exception, if any.
 *
 * @see SingularityException
 */
class DownloadException(msg: String, cause: Throwable? = null) : SingularityException(msg, CODE, cause) {
    companion object {
        const val CODE = "DOWNLOAD_FAILURE"
    }
}
