package io.stereov.singularity.file.download.exception

import io.stereov.singularity.global.exception.SingularityException
import org.springframework.http.HttpStatus

sealed class DownloadException(
    msg: String,
    code: String,
    status: HttpStatus,
    description: String,
    cause: Throwable? = null
) : SingularityException(msg, code, status, description, cause) {

    /**
     * Thrown when the requested file cannot be found at the given URL.
     *
     * This exception extends [DownloadException]
     *
     * @param url The URL of the requested file.
     * @param cause The underlying cause of this exception, if any.
     * @property code `DOWNLOAD_FILE_NOT_FOUND`
     * @property status [HttpStatus.NOT_FOUND]
     */
    class FileNotFound(
        url: String,
        cause: Throwable? = null
    ) : DownloadException(
        msg = "File not found: $url",
        code = "DOWNLOAD_FILE_NOT_FOUND",
        status = HttpStatus.NOT_FOUND,
        description = "Thrown when the requested file cannot be found at the given URL.",
        cause = cause
    )

    /**
     * Thrown when the download request is invalid for the given URL.
     *
     * This exception extends [DownloadException]
     *
     * @param url The URL used for the download request.
     * @param cause The underlying cause of this exception, if any.
     * @property code `DOWNLOAD_BAD_REQUEST`
     * @property status [HttpStatus.BAD_REQUEST]
     */
    class BadRequest(
        url: String,
        cause: Throwable? = null
    ) : DownloadException(
        msg = "Bad request for: $url",
        code = "DOWNLOAD_BAD_REQUEST",
        status = HttpStatus.BAD_REQUEST,
        description = "Thrown when the download request is invalid for the given URL.",
        cause = cause
    )

    /**
     * Thrown when the download request is unauthorized for the given URL.
     *
     * This exception extends [DownloadException]
     *
     * @param url The URL used for the download request.
     * @param cause The underlying cause of this exception, if any.
     * @property code `DOWNLOAD_UNAUTHORIZED`
     * @property status [HttpStatus.UNAUTHORIZED]
     */
    class Unauthorized(
        url: String,
        cause: Throwable? = null
    ) : DownloadException(
        msg = "Unauthorized access to: $url",
        code = "DOWNLOAD_UNAUTHORIZED",
        status = HttpStatus.UNAUTHORIZED,
        description = "Thrown when the download request is unauthorized for the given URL.",
        cause = cause
    )

    /**
     * Thrown when access to the given URL is forbidden.
     *
     * This exception extends [DownloadException]
     *
     * @param url The URL used for the download request.
     * @param cause The underlying cause of this exception, if any.
     * @property code `DOWNLOAD_FORBIDDEN`
     * @property status [HttpStatus.FORBIDDEN]
     */
    class Forbidden(
        url: String,
        cause: Throwable? = null
    ) : DownloadException(
        msg = "Forbidden access to: $url",
        code = "DOWNLOAD_FORBIDDEN",
        status = HttpStatus.FORBIDDEN,
        description = "Thrown when access to the given URL is forbidden.",
        cause = cause
    )

    /**
     * Thrown when a network error occurs while downloading from the given URL.
     *
     * This exception extends [DownloadException]
     *
     * @param url The URL used for the download request.
     * @param cause The underlying cause of this exception, if any.
     * @property code `DOWNLOAD_NETWORK_ERROR`
     * @property status [HttpStatus.SERVICE_UNAVAILABLE]
     */
    class NetworkError(
        url: String,
        cause: Throwable? = null
    ) : DownloadException(
        msg = "Network error while downloading $url",
        code = "DOWNLOAD_NETWORK_ERROR",
        status = HttpStatus.SERVICE_UNAVAILABLE,
        description = "Thrown when a network error occurs while downloading from the given URL.",
        cause = cause
    )

    /**
     * Thrown when the file at the given URL is too large to be downloaded.
     *
     * This exception extends [DownloadException]
     *
     * @param url The URL of the file that is too large.
     * @param cause The underlying cause of this exception, if any.
     * @property code `DOWNLOAD_FILE_TOO_LARGE`
     * @property status [HttpStatus.PAYLOAD_TOO_LARGE]
     */
    class FileTooLarge(
        url: String,
        cause: Throwable? = null
    ) : DownloadException(
        msg = "File too large: $url",
        code = "DOWNLOAD_FILE_TOO_LARGE",
        status = HttpStatus.PAYLOAD_TOO_LARGE,
        description = "Thrown when the file at the given URL is too large to be downloaded.",
        cause = cause
    )

    /**
     * Thrown when downloading from the given URL times out.
     *
     * This exception extends [DownloadException]
     *
     * @param url The URL used for the download request.
     * @param cause The underlying cause of this exception, if any.
     * @property code `DOWNLOAD_TIMEOUT`
     * @property status [HttpStatus.GATEWAY_TIMEOUT]
     */
    class Timeout(
        url: String,
        cause: Throwable? = null
    ) : DownloadException(
        msg = "Timeout while downloading $url",
        code = "DOWNLOAD_TIMEOUT",
        status = HttpStatus.GATEWAY_TIMEOUT,
        description = "Thrown when downloading from the given URL times out.",
        cause = cause
    )

    /**
     * Thrown when an internal error occurs while downloading from the given URL.
     *
     * This exception extends [DownloadException]
     *
     * @param url The URL used for the download request.
     * @param cause The underlying cause of this exception, if any.
     * @property code `DOWNLOAD_INTERNAL_ERROR`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class InternalError(
        url: String,
        cause: Throwable? = null
    ) : DownloadException(
        msg = "Internal error for $url",
        code = "DOWNLOAD_INTERNAL_ERROR",
        status = HttpStatus.INTERNAL_SERVER_ERROR,
        description = "Thrown when an internal error occurs while downloading from the given URL.",
        cause = cause
    )

    /**
     * Thrown when an unknown error occurs while downloading from the given URL.
     *
     * This exception extends [DownloadException]
     *
     * @param url The URL used for the download request.
     * @param cause The underlying cause of this exception, if any.
     * @property code `DOWNLOAD_UNKNOWN_ERROR`
     * @property status [HttpStatus.INTERNAL_SERVER_ERROR]
     */
    class Unknown(
        url: String,
        cause: Throwable? = null
    ) : DownloadException(
        msg = "Unknown error for $url",
        code = "DOWNLOAD_UNKNOWN_ERROR",
        status = HttpStatus.INTERNAL_SERVER_ERROR,
        description = "Thrown when an unknown error occurs while downloading from the given URL.",
        cause = cause
    )
}